package com.wrap.domain.schedule.dto;

import java.time.LocalDateTime;

public record DeadlineSummaryHeaderResponse(
        Long scheduleId,
        long daysLeft,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int readyPercent
) {
}
