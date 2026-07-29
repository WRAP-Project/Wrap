package com.wrap.domain.schedule.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ScheduleUpdateRequest(
        Long projectId,

        @Size(max = 100, message = "일정 제목은 100자 이하여야 합니다.")
        String title,

        String description,

        LocalDateTime startAt,

        LocalDateTime endAt,

        Boolean shared
) {

    @AssertTrue(message = "종료 시간은 시작 시간보다 이후여야 합니다.")
    public boolean isValidDateRange() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return endAt.isAfter(startAt);
    }
}
