package com.wrap.domain.calendar.dto;

import com.wrap.domain.calendar.enums.CalendarRiskLevel;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CalendarRiskCheckResponse(
        Long taskId,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        Long assigneeProjectMemberId,
        String assigneeNickname,
        ProjectMemberRole assigneeRole,
        CalendarRiskLevel riskLevel,
        String reason
) {

    public static CalendarRiskCheckResponse from(Task task, LocalDate today) {
        ProjectMember assignee = task.getAssignee();
        CalendarRiskLevel riskLevel = resolveRiskLevel(task, today);

        return new CalendarRiskCheckResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getMember().getNickname(),
                assignee == null ? null : assignee.getRole(),
                riskLevel,
                resolveReason(riskLevel)
        );
    }

    private static CalendarRiskLevel resolveRiskLevel(Task task, LocalDate today) {
        if (task.getStatus() == TaskStatus.HOLD) {
            return CalendarRiskLevel.BLOCKED;
        }
        if (task.getDueDate() != null && task.getDueDate().isBefore(today)) {
            return CalendarRiskLevel.OVERDUE;
        }
        if (task.getDueDate() != null && ChronoUnit.DAYS.between(today, task.getDueDate()) <= 7) {
            return CalendarRiskLevel.DUE_SOON;
        }
        return CalendarRiskLevel.NEEDS_CHECK;
    }

    private static String resolveReason(CalendarRiskLevel riskLevel) {
        return switch (riskLevel) {
            case BLOCKED -> "Task is on hold.";
            case OVERDUE -> "Task is overdue.";
            case DUE_SOON -> "Task is due soon.";
            case NEEDS_CHECK -> "Task status needs checking.";
        };
    }
}
