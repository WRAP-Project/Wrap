package com.wrap.domain.schedule.dto;

public record DeadlineSummaryUpdateResponse(
        String author,
        String text,
        String time
) {
}
