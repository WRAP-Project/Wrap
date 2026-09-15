package com.wrap.domain.schedule.dto;

public record DeadlineSummaryStatsResponse(
        long checklistDone,
        long checklistTotal,
        long fileCount,
        long participantCount
) {
}
