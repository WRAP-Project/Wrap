import os
import random
import time
from typing import Dict, List, Optional, Tuple

import requests
from google import genai


class GeminiCodeReview:
    def __init__(self):
        self.github_token = os.getenv("GITHUB_TOKEN")
        self.api_key = os.getenv("GEMINI_API_KEY")
        self.pr_number = os.getenv("PR_NUMBER")
        self.repo = os.getenv("GITHUB_REPOSITORY")
        self.model = os.getenv("GEMINI_MODEL", "gemini-3.5-flash")

        self.priority = {
            "entity": 1,
            "repository": 2,
            "service": 3,
            "controller": 4,
            "dto": 5,
            "config": 6,
            "test": 7,
            "docs": 8,
        }

        if not all([self.github_token, self.api_key, self.pr_number, self.repo]):
            raise ValueError(
                "Required environment variables are missing: "
                "GITHUB_TOKEN, GEMINI_API_KEY, PR_NUMBER, GITHUB_REPOSITORY"
            )

    def github_headers(self) -> Dict[str, str]:
        return {
            "Authorization": f"Bearer {self.github_token}",
            "Accept": "application/vnd.github.v3+json",
            "X-GitHub-Api-Version": "2022-11-28",
        }

    def get_pr_files(self) -> List[Dict]:
        files = []
        page = 1

        while True:
            url = (
                f"https://api.github.com/repos/{self.repo}/pulls/"
                f"{self.pr_number}/files?per_page=100&page={page}"
            )
            response = requests.get(url, headers=self.github_headers(), timeout=20)

            if response.status_code != 200:
                raise RuntimeError(f"Failed to fetch PR files: {response.text}")

            page_files = response.json()
            files.extend(page_files)

            if len(page_files) < 100:
                return files

            page += 1

    def classify_file(self, file_path: str) -> str:
        normalized = file_path.replace("\\", "/")

        if normalized.startswith(".github/") or normalized.endswith((".yml", ".yaml")):
            return "config"
        if normalized.endswith((".md", ".txt")):
            return "docs"
        if "/src/test/" in normalized and normalized.endswith(".java"):
            return "test"
        if not normalized.endswith(".java"):
            return "ignore"
        if "/entity/" in normalized:
            return "entity"
        if "/repository/" in normalized:
            return "repository"
        if "/service/" in normalized:
            return "service"
        if "/controller/" in normalized:
            return "controller"
        if "/dto/" in normalized:
            return "dto"
        if "/global/" in normalized:
            return "config"

        return "ignore"

    def extract_meaningful_diff(self, patch: Optional[str]) -> str:
        if not patch:
            return ""

        filtered = []
        for line in patch.splitlines():
            if line.startswith(("+++", "---")):
                continue
            if line.startswith(("+", "-")):
                filtered.append(line)

        return "\n".join(filtered)

    def split_chunks(self, code: str, max_size: int = 5000) -> List[str]:
        return [code[i : i + max_size] for i in range(0, len(code), max_size)]

    def build_prompt(self, file_path: str, code: str, category: str) -> str:
        return f"""
당신은 Wrap 프로젝트의 Spring Boot 백엔드 Pull Request를 리뷰하는 시니어 백엔드 개발자입니다.

반드시 한국어로 리뷰하세요.

diff에서 확인되는 구체적인 위험만 리뷰하세요. 버그, 보안 문제, 잘못된 JPA 매핑,
깨질 수 있는 Spring Data Repository 메서드, 누락된 검증, 트랜잭션 경계 문제,
API 계약 문제, 테스트 공백을 우선적으로 확인하세요.

칭찬이나 일반적인 요약은 작성하지 마세요. 변경되지 않은 코드를 설명하지 마세요.
의미 있는 문제가 없으면 "차단 이슈 없음."이라고만 작성하세요. 리뷰는 간결하게 작성하세요.

프로젝트 컨텍스트:
- Wrap은 단기 프로젝트 운영 플랫폼입니다.
- Mission은 Project의 goal, successCriteria 필드로 저장합니다.
- Deliverable은 Task.deliverable로 표현합니다.
- Task.assignee는 Member가 아니라 ProjectMember를 참조해야 합니다.

Review category: {category}
File: {file_path}

Diff:
```diff
{code}
```
"""

    def retry_with_backoff(self, func, max_retries: int = 5, base_delay: float = 1.0):
        for attempt in range(max_retries):
            try:
                return func()
            except Exception as error:
                is_retryable = "429" in str(error) or "503" in str(error) or "UNAVAILABLE" in str(error)
                is_last = attempt == max_retries - 1

                if not is_retryable or is_last:
                    raise

                sleep_seconds = base_delay * (2**attempt) + random.uniform(0, 0.5)
                print(f"Gemini request retry in {sleep_seconds:.2f}s ({attempt + 1}/{max_retries})")
                time.sleep(sleep_seconds)

    def call_gemini(self, prompt: str) -> str:
        client = genai.Client(api_key=self.api_key)

        def request():
            response = client.models.generate_content(
                model=self.model,
                contents=prompt,
            )
            return getattr(response, "text", "") or "No response from Gemini."

        return self.retry_with_backoff(request)

    def create_review_comment(self, review: str):
        url = f"https://api.github.com/repos/{self.repo}/issues/{self.pr_number}/comments"
        data = {"body": f"## Gemini 코드 리뷰\n\n{review}"}
        response = requests.post(url, headers=self.github_headers(), json=data, timeout=20)

        if response.status_code != 201:
            raise RuntimeError(f"Failed to create PR comment: {response.text}")

    def get_review_targets(self) -> List[Tuple[str, Dict]]:
        filtered = []

        for file in self.get_pr_files():
            category = self.classify_file(file["filename"])
            if category == "ignore":
                continue
            if file.get("status") == "removed":
                continue
            if file.get("additions", 0) + file.get("deletions", 0) < 3:
                continue

            filtered.append((category, file))

        return sorted(filtered, key=lambda item: self.priority.get(item[0], 999))

    def run(self):
        print("Starting Gemini code review")

        reviews = []
        for category, file in self.get_review_targets():
            diff = self.extract_meaningful_diff(file.get("patch"))
            if not diff.strip():
                continue

            for index, chunk in enumerate(self.split_chunks(diff), start=1):
                prompt = self.build_prompt(file["filename"], chunk, category)
                review = self.call_gemini(prompt)
                chunk_label = f" chunk {index}" if len(diff) > 5000 else ""
                reviews.append(f"### `{file['filename']}`{chunk_label}\n\n{review}")

        if reviews:
            self.create_review_comment("\n\n".join(reviews))
            print("Gemini review comment created")
        else:
            print("No reviewable files found")


if __name__ == "__main__":
    GeminiCodeReview().run()
