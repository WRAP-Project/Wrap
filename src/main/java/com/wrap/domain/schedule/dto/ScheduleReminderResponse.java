package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ScheduleReminderResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean shared,
        long daysLeft
) {

    public static ScheduleReminderResponse from(Schedule schedule, LocalDate today) {
        return new ScheduleReminderResponse(
                schedule.getId(),
                schedule.getProject().getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isShared(),
                ChronoUnit.DAYS.between(today, schedule.getStartAt().toLocalDate())
        );
    }
}
