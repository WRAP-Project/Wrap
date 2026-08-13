package com.wrap.domain.schedule.dto;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record RiskCheckResponse(
        Long taskId,
        Long projectId,
        String projectName,
        Long assigneeProjectMemberId,
        String assigneeNickname,
        String title,
        TaskStatus status,
        LocalDate dueDate,
        Long daysLeft,
        int progress
) {

    public static RiskCheckResponse from(Task task, LocalDate today) {
        ProjectMember assignee = task.getAssignee();
        LocalDate dueDate = task.getDueDate();
        return new RiskCheckResponse(
                task.getId(),
                task.getProject().getId(),
                task.getProject().getName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getMember().getNickname(),
                task.getTitle(),
                task.getStatus(),
                dueDate,
                dueDate == null ? null : ChronoUnit.DAYS.between(today, dueDate),
                task.getProgress()
        );
    }
}
