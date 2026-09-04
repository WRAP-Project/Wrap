# PR: 프론트 캘린더 응답 통일 수정

## Summary

- Schedule 응답에 프론트 캘린더가 사용하는 `type`, `reminder` 필드를 추가했습니다.
- 가능 시간 요청에 `startTime`, `endTime`을 추가하고, 제출/확정 슬롯이 요청 시간 범위를 벗어나지 않도록 검증했습니다.
- 캘린더 막힘 신호 화면을 위한 `GET /projects/{projectId}/calendar/risk-checks` API를 추가했습니다.

## Changes

### Schedule

- `ScheduleType` enum을 추가했습니다.
  - JSON 값은 프론트 타입과 맞추기 위해 `deadline`, `meeting`, `milestone` 소문자로 내려갑니다.
  - 요청에서는 대소문자 enum 이름과 소문자 값을 모두 받을 수 있습니다.
- `Schedule` 엔티티에 `schedule_type`, `reminder_enabled` 컬럼을 추가했습니다.
- `ScheduleCreateRequest`, `ScheduleUpdateRequest`, `ScheduleResponse`, `ScheduleDetailResponse`, `ScheduleReminderResponse`에 `type`, `reminder`를 반영했습니다.
- 기존 요청 호환을 위해 `type` 미전달 시 `meeting`, `reminder` 미전달 시 `false`로 처리합니다.

### Availability

- `AvailabilityRequest`에 `startTime`, `endTime`을 추가했습니다.
- 가능 시간 요청 생성/목록/상세/내 응답 조회 응답에 시간 범위를 포함했습니다.
- 슬롯 제출 및 추천 시간 확정 시 슬롯이 날짜 범위뿐 아니라 `startTime`~`endTime` 범위 안에 있는지 검증합니다.
- 기존 요청 호환을 위해 시간 미전달 시 `00:00:00`~`23:59:59`로 처리합니다.

### Calendar Risk Checks

- `GET /projects/{projectId}/calendar/risk-checks` API를 추가했습니다.
- Query parameter:
  - `status`
  - `assigneeProjectMemberId`
  - `dueFrom`
  - `dueTo`
- 응답은 Task 기반 리스크 신호입니다.
  - `HOLD` 상태는 `BLOCKED`
  - 마감일이 지난 미완료 Task는 `OVERDUE`
  - 7일 이내 마감 Task는 `DUE_SOON`
  - 그 외 미완료 Task는 `NEEDS_CHECK`
- `DONE` Task는 리스크 목록에서 제외합니다.

## Test

- 추가/수정한 테스트:
  - `ScheduleServiceTest#createScheduleIncludesTypeAndReminderFlag`
  - `AvailabilityServiceTest#createStoresAvailabilityTimeRange`
  - `AvailabilityServiceTest#upsertMyResponseRejectsSlotOutsideRequestTimeRange`
  - `CalendarServiceTest#findRiskChecksReturnsTaskRiskSignals`
  - Swagger path 확인에 `/projects/{projectId}/calendar/risk-checks` 추가
- 로컬 실행 결과:
  - `git diff --check`: 통과
  - `npm run build` in `Wrap-client`: 통과
  - `.\\gradlew.bat test`: 현재 환경에서 Gradle daemon loopback 연결 실패로 실행 불가

## Frontend Notes

- 프론트 캘린더 화면은 현재 mock 상태라 새 API를 직접 호출하지 않습니다.
- 백엔드 머지 후 프론트에서 다음 순서로 타입을 갱신하면 됩니다.
  - 백엔드 서버 실행
  - `npm run sync:api`
  - `npm run codegen:api`
- 프로젝트 ID는 백엔드가 `Long`을 유지하므로 프론트의 `srv-{id}` 접두사 제거 후 숫자로 호출해야 합니다.
