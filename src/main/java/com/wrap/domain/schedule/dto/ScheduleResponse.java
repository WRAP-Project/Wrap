package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long projectId,
        Long creatorId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean shared
) {

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
                schedule.isShared()
        );
    }
}
