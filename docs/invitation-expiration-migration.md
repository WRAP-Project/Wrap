# 초대 만료 정책 운영 DB 적용

이메일 초대와 프로젝트 초대 링크는 생성 시각부터 정확히 7일간 유효합니다.
만료 여부의 기준은 각 테이블의 `expires_at`이며, 이메일 초대의 `EXPIRED` 상태는
목록 표시와 이력 구분에 사용합니다.

## 적용 정책

- `invitation.expires_at`: `created_at + 7일`
- `project_invite_link.expires_at`: `created_at + 7일`
- 기존 데이터에도 별도의 유예기간 없이 같은 기준 적용
- 기존 이메일 초대 중 이미 만료된 `INVITED` 행은 `EXPIRED`로 변경
- 기존 초대 링크의 `active`는 변경하지 않음
  - `active`는 관리자가 링크를 비활성화했는지를 나타냄
  - 실제 사용 가능 여부는 `active = true`이면서 `expires_at > 현재 시각`인지로 판단

## 배포 전 확인

운영 DB를 백업하고 컬럼 존재 여부를 확인합니다.

```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('invitation', 'project_invite_link')
  AND COLUMN_NAME = 'expires_at';
```

두 테이블 모두 조회되지 않을 때 다음 순서로 적용합니다. 기존 행을 먼저 보정해야 하므로
처음에는 nullable 컬럼으로 추가한 뒤 마지막에 NOT NULL로 변경합니다.

```sql
ALTER TABLE invitation
    ADD COLUMN expires_at DATETIME(6) NULL;

ALTER TABLE project_invite_link
    ADD COLUMN expires_at DATETIME(6) NULL;

UPDATE invitation
SET expires_at = DATE_ADD(created_at, INTERVAL 7 DAY)
WHERE expires_at IS NULL;

UPDATE project_invite_link
SET expires_at = DATE_ADD(created_at, INTERVAL 7 DAY)
WHERE expires_at IS NULL;

UPDATE invitation
SET status = 'EXPIRED'
WHERE status = 'INVITED'
  AND expires_at <= CURRENT_TIMESTAMP(6);

ALTER TABLE invitation
    MODIFY COLUMN expires_at DATETIME(6) NOT NULL;

ALTER TABLE project_invite_link
    MODIFY COLUMN expires_at DATETIME(6) NOT NULL;
```

한 테이블에만 컬럼이 존재하는 경우에는 존재하지 않는 테이블의 `ADD COLUMN`만 실행하고,
두 테이블의 보정 `UPDATE`와 `MODIFY COLUMN`을 실행합니다.

운영 설정은 현재 `spring.jpa.hibernate.ddl-auto=update`이지만, 기존 행이 있는 테이블에
NOT NULL 컬럼을 안전하게 추가하기 위해 애플리케이션 배포 전에 위 SQL을 명시적으로
적용해야 합니다.

## 배포 후 확인

```sql
SELECT COUNT(*) AS missing_expiration_count
FROM invitation
WHERE expires_at IS NULL;

SELECT COUNT(*) AS missing_expiration_count
FROM project_invite_link
WHERE expires_at IS NULL;

SELECT COUNT(*) AS stale_pending_invitation_count
FROM invitation
WHERE status = 'INVITED'
  AND expires_at <= CURRENT_TIMESTAMP(6);

SELECT id, created_at, expires_at, status
FROM invitation
ORDER BY id DESC
LIMIT 20;

SELECT id, created_at, expires_at, active, revoked_at
FROM project_invite_link
ORDER BY id DESC
LIMIT 20;
```

각 `missing_expiration_count`와 `stale_pending_invitation_count`는 모두 `0`이어야 합니다.
롤백하더라도 `expires_at` 컬럼을 즉시 삭제하지 않아 기존 만료 정보의 손실을 방지합니다.
