# Render 백엔드 배포 가이드

Wrap 백엔드는 Docker 기반 Render Web Service와 외부 MySQL 8을 사용합니다.
운영 비밀번호와 실제 연결 정보는 저장소에 커밋하지 않고 Render 환경변수로 관리합니다.

## 1. 사전 준비

- 배포할 변경사항이 `develop` 브랜치에 병합되어 있어야 합니다.
- 외부에서 접속할 수 있는 MySQL 8 데이터베이스와 전용 사용자를 준비합니다.
- MySQL 서비스가 IP 접근을 제한한다면 Render Web Service의 Outbound IP 범위를 허용합니다.
- 배포된 프론트엔드의 Origin을 확인합니다.

Origin은 경로나 마지막 `/`를 포함하지 않습니다.

```text
https://frontend.example.com
```

## 2. Render Web Service 생성

Render Dashboard에서 `New` > `Web Service`를 선택하고 GitHub 저장소를 연결합니다.

| 항목 | 값 |
| --- | --- |
| Branch | `develop` |
| Runtime | `Docker` |
| Root Directory | 비워둠 |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/actuator/health` |

인스턴스 유형과 리전은 팀에서 선택합니다. 외부 MySQL과 가까운 리전을 사용하면 네트워크 지연을 줄일 수 있습니다.

## 3. 환경변수 등록

Render 서비스의 `Environment`에 다음 값을 등록합니다.

| Key | 값 또는 형식 | 비밀값 여부 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | 아니요 |
| `DB_URL` | `jdbc:mysql://<host>:<port>/<database>` | 예 |
| `DB_USERNAME` | MySQL 전용 사용자 이름 | 예 |
| `DB_PASSWORD` | MySQL 전용 사용자 비밀번호 | 예 |
| `FRONTEND_ORIGIN` | 배포된 프론트엔드 Origin | 아니요 |

`PORT`는 Render가 Web Service에 자동으로 제공하므로 직접 등록하지 않습니다. 애플리케이션은 값이 없을 때만 `8080`을 기본값으로 사용합니다.

MySQL 공급자가 TLS 옵션이나 별도 JDBC 파라미터를 요구하면 해당 공급자가 안내한 값을 `DB_URL`에 추가합니다.

## 4. 첫 배포 확인

배포 로그에서 다음 항목을 순서대로 확인합니다.

1. Docker 이미지 빌드 성공
2. Spring의 `prod` 프로필 활성화
3. MySQL 연결 성공
4. 내장 서버가 Render의 `PORT`에서 시작
5. Health Check 통과

배포가 완료되면 다음 주소가 HTTP 200과 `UP` 상태를 반환해야 합니다.

```text
https://<render-service>.onrender.com/actuator/health
```

```json
{
  "status": "UP"
}
```

## 5. 프론트엔드 연동

프론트엔드의 API 기본 주소를 Render 백엔드 URL로 변경합니다. 로그인 세션을 사용하는 모든 요청에는 자격 증명 전송 설정이 필요합니다.

```javascript
fetch(apiUrl, {
  credentials: "include"
});
```

Axios를 사용한다면 `withCredentials: true`를 설정합니다.

운영 세션 쿠키에는 `Secure`, `HttpOnly`, `SameSite=None`이 적용됩니다. 따라서 운영 연동은 HTTPS 주소를 사용해야 합니다.

## 6. 장애 확인 순서

- 서버가 시작되지 않으면 `SPRING_PROFILES_ACTIVE`와 필수 DB 환경변수를 확인합니다.
- Health Check가 실패하면 Render 로그와 MySQL 연결 상태를 확인합니다.
- DB 연결이 거절되면 MySQL 방화벽과 Render Outbound IP 허용 여부를 확인합니다.
- 브라우저에서 CORS 오류가 발생하면 `FRONTEND_ORIGIN`이 실제 Origin과 정확히 일치하는지 확인합니다.
- 로그인 후 세션이 유지되지 않으면 프론트엔드의 credentials 설정과 브라우저 쿠키 정책을 확인합니다.
