package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.enums.ScheduleType;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long projectId,
        Long creatorId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean shared,
        ScheduleType type,
        boolean reminder
) {

    public ScheduleResponse(
            Long id,
            Long projectId,
            Long creatorId,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean shared
    ) {
        this(id, projectId, creatorId, title, description, startAt, endAt, shared, ScheduleType.MEETING, false);
    }

    public static ScheduleResponse from(Schedule schedule) {
        Long projectId = schedule.getProject() == null ? null : schedule.getProject().getId();
        return new ScheduleResponse(
                schedule.getId(),
                projectId,
                schedule.getCreator().getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isShared(),
                schedule.getType(),
                schedule.isReminder()
        );
    }
}
