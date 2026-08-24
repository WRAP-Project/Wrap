package com.wrap.domain.schedule.dto;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;

public record ScheduleDetailResponse(
        Long id,
        Long projectId,
        String projectName,
        Long creatorId,
        String creatorNickname,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean shared,
        boolean checked
) {

    public static ScheduleDetailResponse from(Schedule schedule, boolean checked) {
        Project project = schedule.getProject();
        Long projectId = project == null ? null : project.getId();
        String projectName = project == null ? null : project.getName();

        return new ScheduleDetailResponse(
                schedule.getId(),
                projectId,
                projectName,
                schedule.getCreator().getId(),
                schedule.getCreator().getNickname(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isShared(),
                checked
        );
    }
}
