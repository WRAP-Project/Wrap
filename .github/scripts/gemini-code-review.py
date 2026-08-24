import os
import random
import re
import time
from typing import Dict, List, Optional, Tuple

import requests
from google import genai


class GeminiCodeReview:
    BATCH_SIZE = 4
    MAX_REVIEW_FILES = 30
    MIN_CALL_INTERVAL = 5.0

    def __init__(self):
        self.github_token = os.getenv("GITHUB_TOKEN")
        self.api_key = os.getenv("GEMINI_API_KEY")
        self.pr_number = os.getenv("PR_NUMBER")
        self.repo = os.getenv("GITHUB_REPOSITORY")
        self.model = os.getenv("GEMINI_MODEL", "gemini-3.5-flash")
        self.last_call_at: Optional[float] = None

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

        if normalized.startswith(".github/"):
            return "config"
        if normalized.endswith((".gradle", ".properties", ".yml", ".yaml")):
            return "config"
        if normalized.startswith("src/main/resources/"):
            return "config"
        if normalized.endswith((".md", ".txt")):
            return "docs"
        if normalized.startswith("src/test/") and normalized.endswith(".java"):
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

    def extract_file_diff(self, patch: Optional[str], max_size: int = 12000) -> str:
        if not patch:
            return ""

        diff = patch.strip()
        if len(diff) <= max_size:
            return diff

        return (
            diff[:max_size]
            + "\n\n... diff가 길어 일부만 포함되었습니다. 위 범위 기준으로 파일 단위 요약 리뷰를 작성하세요."
        )

    def build_batch_prompt(self, items: List[Tuple[str, str, str]]) -> str:
        files_block = "\n\n".join(
            f"""---
리뷰 분류: {category}
파일: {file_path}

Diff:
```diff
{code}
```"""
            for file_path, code, category in items
        )

        return f"""
당신은 Wrap 프로젝트의 Spring Boot 백엔드 Pull Request를 리뷰하는 시니어 백엔드 개발자입니다.

반드시 한국어로 답변하세요.
리뷰 방식은 "전체 변경사항에 대한 파일 단위 요약 리뷰"입니다.
아래에는 여러 파일의 diff가 `---`로 구분되어 이어져 있습니다. 각 파일마다 독립적으로 무엇이 바뀌었고, 그 변경이 어떤 영향을 주는지 요약한 뒤 필요한 리뷰 의견만 작성하세요.

각 파일마다 반드시 아래 형식을 반복해서 출력하세요. 파일 순서와 개수는 입력과 동일해야 합니다.

### `파일 경로`

**변경 요약**
- 이 파일에서 바뀐 내용을 1~3개 bullet로 요약

**영향 범위**
- 이 변경이 영향을 주는 기능, 계층, 실행 흐름을 1~3개 bullet로 요약

**확인 필요**
- 실제 버그, 설계 리스크, 누락된 검증, 테스트 공백이 있으면 bullet로 작성
- 확인할 문제가 없으면 `- 없음`으로 작성

**제안**
- 바로 반영하면 좋은 개선이 있으면 bullet로 작성
- 제안할 내용이 없으면 `- 없음`으로 작성

리뷰 기준:
- 변경되지 않은 코드는 길게 설명하지 마세요.
- 단순 칭찬은 작성하지 마세요.
- 추측성 지적은 피하고 diff에서 근거가 보이는 내용만 작성하세요.
- Java, Spring Boot, JPA, Spring Data Repository, Gradle 설정 관점에서 확인하세요.
- 파일 간 내용을 섞지 말고, 각 파일은 자신의 diff만 근거로 리뷰하세요.

프로젝트 컨텍스트:
- Wrap은 단기 프로젝트 운영 플랫폼입니다.
- Mission은 Project의 goal, successCriteria 필드로 저장합니다.
- Deliverable은 Task.deliverable로 표현합니다.
- Task.assignee는 Member가 아니라 ProjectMember를 참조해야 합니다.

리뷰 대상 파일 목록:

{files_block}
"""

    def extract_retry_delay(self, error: Exception) -> Optional[float]:
        match = re.search(r"retryDelay['\"]?\s*[:=]\s*['\"]?(\d+(?:\.\d+)?)s", str(error))
        if match:
            return float(match.group(1))
        return None

    def retry_with_backoff(self, func, max_retries: int = 5, base_delay: float = 1.0):
        for attempt in range(max_retries):
            try:
                return func()
            except Exception as error:
                is_quota_exhausted = "429" in str(error) or "RESOURCE_EXHAUSTED" in str(error)
                is_retryable = is_quota_exhausted or "503" in str(error) or "UNAVAILABLE" in str(error)
                is_last = attempt == max_retries - 1

                if not is_retryable or is_last:
                    raise

                if is_quota_exhausted:
                    sleep_seconds = self.extract_retry_delay(error) or 60.0
                else:
                    sleep_seconds = base_delay * (2**attempt) + random.uniform(0, 0.5)

                print(f"Gemini request retry in {sleep_seconds:.2f}s ({attempt + 1}/{max_retries})")
                time.sleep(sleep_seconds)

    def throttle(self):
        if self.last_call_at is not None:
            elapsed = time.monotonic() - self.last_call_at
            wait = self.MIN_CALL_INTERVAL - elapsed
            if wait > 0:
                time.sleep(wait)

        self.last_call_at = time.monotonic()

    def call_gemini(self, prompt: str) -> str:
        client = genai.Client(api_key=self.api_key)

        def request():
            self.throttle()
            response = client.models.generate_content(
                model=self.model,
                contents=prompt,
            )
            return getattr(response, "text", "") or "Gemini 응답이 없습니다."

        return self.retry_with_backoff(request)

    def create_review_comment(self, review: str):
        url = f"https://api.github.com/repos/{self.repo}/issues/{self.pr_number}/comments"
        data = {"body": f"## Gemini 파일 단위 요약 리뷰\n\n{review}"}
        response = requests.post(url, headers=self.github_headers(), json=data, timeout=20)

        if response.status_code != 201:
            raise RuntimeError(f"Failed to create PR comment: {response.text}")

    def get_review_targets(self) -> List[Tuple[str, Dict]]:
        filtered = []

        for file in self.get_pr_files():
            category = self.classify_file(file["filename"])
            if category == "ignore":
                continue

            filtered.append((category, file))

        return sorted(filtered, key=lambda item: self.priority.get(item[0], 999))

    def run(self):
        print("Starting Gemini file summary review")

        targets = []
        for category, file in self.get_review_targets():
            diff = self.extract_file_diff(file.get("patch"))
            if not diff.strip():
                continue
            targets.append((file["filename"], diff, category))

        skipped_count = max(0, len(targets) - self.MAX_REVIEW_FILES)
        targets = targets[: self.MAX_REVIEW_FILES]

        reviews = []
        for i in range(0, len(targets), self.BATCH_SIZE):
            batch = targets[i : i + self.BATCH_SIZE]
            batch_names = [file_path for file_path, _, _ in batch]

            try:
                prompt = self.build_batch_prompt(batch)
                review = self.call_gemini(prompt)
                reviews.append(review)
            except Exception as error:
                print(f"Gemini batch review failed for {batch_names}: {error}")
                failed_list = "\n".join(f"- `{name}`" for name in batch_names)
                reviews.append(f"⚠️ 아래 파일들은 리뷰 생성에 실패했습니다.\n\n{failed_list}")

        if skipped_count:
            reviews.append(f"⚠️ 파일 수가 많아 {skipped_count}개 파일은 리뷰가 생략되었습니다.")

        if reviews:
            self.create_review_comment("\n\n".join(reviews))
            print("Gemini file summary review comment created")
        else:
            print("No reviewable files found")


if __name__ == "__main__":
    GeminiCodeReview().run()
