package com.wrap.domain.task.dto;

import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        Long projectId,
        Long milestoneId,
        TaskAssigneeResponse assignee,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        int progress,
        TaskPriority priority,
        boolean deliverable
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getMilestone() == null ? null : task.getMilestone().getId(),
                TaskAssigneeResponse.from(task.getAssignee()),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getProgress(),
                task.getPriority(),
                task.isDeliverable()
        );
    }
}
