package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record DeadlineSummaryResponse(
        DeadlineSummaryHeaderResponse header,
        DeadlineSummaryStatsResponse stats,
        List<ScheduleReminderChecklistItemResponse> checklist,
        List<String> attachments,
        DeadlineSummaryUpdateResponse recentUpdate
) {

    public static DeadlineSummaryResponse from(
            Schedule schedule,
            LocalDate today,
            List<ScheduleReminderChecklistItemResponse> checklist
    ) {
        long doneCount = checklist.stream()
                .filter(item -> item.status()
                        == ScheduleReminderChecklistItemResponse.ReminderChecklistStatus.DONE)
                .count();
        int readyPercent = checklist.isEmpty()
                ? 0
                : (int) Math.round(doneCount * 100.0 / checklist.size());
        long participantCount = checklist.stream()
                .map(ScheduleReminderChecklistItemResponse::assigneeNickname)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        return new DeadlineSummaryResponse(
                new DeadlineSummaryHeaderResponse(
                        schedule.getId(),
                        Math.max(0, ChronoUnit.DAYS.between(today, schedule.getEndAt().toLocalDate())),
                        schedule.getTitle(),
                        schedule.getStartAt(),
                        schedule.getEndAt(),
                        readyPercent
                ),
                new DeadlineSummaryStatsResponse(
                        doneCount,
                        checklist.size(),
                        0,
                        participantCount
                ),
                checklist,
                List.of(),
                null
        );
    }
}
