package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.enums.ScheduleType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record ScheduleReminderResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean shared,
        ScheduleType type,
        boolean reminder,
        long daysLeft,
        List<ScheduleReminderChecklistItemResponse> checklist
) {

    public static ScheduleReminderResponse from(
            Schedule schedule,
            LocalDate today,
            List<ScheduleReminderChecklistItemResponse> checklist
    ) {
        return new ScheduleReminderResponse(
                schedule.getId(),
                schedule.getProject().getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isShared(),
                schedule.getType(),
                schedule.isReminder(),
                ChronoUnit.DAYS.between(today, schedule.getEndAt().toLocalDate()),
                checklist
        );
    }
}
