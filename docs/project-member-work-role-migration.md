# 프로젝트 멤버 업무 역할 운영 DB 적용

`ProjectMember.workRole`은 프로젝트별 업무 역할을 저장하는 nullable 문자열 컬럼입니다.
기존 `role` 컬럼의 OWNER/MEMBER 관리 권한과 별도로 유지합니다.

## 적용 정책

- 컬럼: `project_member.work_role`
- 타입: `VARCHAR(50) NULL`
- 기존 참여자: 일괄 보정하지 않고 `NULL`로 유지
- 신규 참여자: 최초 참여 시점의 `member.role`을 애플리케이션에서 복사
- 재참여자: 기존 `work_role` 유지

## 배포 전 확인

운영 DB를 백업하고 컬럼 존재 여부를 확인합니다.

```sql
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'project_member'
  AND COLUMN_NAME = 'work_role';
```

조회 결과가 없을 때만 다음 DDL을 실행합니다.

```sql
ALTER TABLE project_member
    ADD COLUMN work_role VARCHAR(50) NULL;
```

운영 설정은 현재 `spring.jpa.hibernate.ddl-auto=update`이지만, 배포 시점의 자동 DDL에만
의존하지 않도록 애플리케이션 배포 전에 위 변경을 명시적으로 적용하는 것을 권장합니다.

## 배포 후 확인

```sql
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'project_member'
  AND COLUMN_NAME = 'work_role';

SELECT COUNT(*) AS existing_members,
       SUM(work_role IS NULL) AS unassigned_members
FROM project_member;
```

기존 행의 `work_role`이 `NULL`인 것은 의도된 상태이며 화면에서는 `역할 미지정`으로 표시합니다.
애플리케이션을 되돌리더라도 nullable 컬럼은 즉시 삭제하지 않고 남겨 두어 기존 값 손실을
방지합니다.
