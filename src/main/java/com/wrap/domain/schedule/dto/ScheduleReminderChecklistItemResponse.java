package com.wrap.domain.schedule.dto;

import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.enums.MilestoneStatus;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import java.time.LocalDate;

public record ScheduleReminderChecklistItemResponse(
        String id,
        ReminderChecklistSourceType sourceType,
        Long sourceId,
        String title,
        ReminderChecklistStatus status,
        String statusLabel,
        String assigneeNickname,
        ProjectMemberRole assigneeRole,
        LocalDate dueDate
) {

    public static ScheduleReminderChecklistItemResponse from(Milestone milestone) {
        ReminderChecklistStatus status = milestone.getStatus() == MilestoneStatus.DONE
                ? ReminderChecklistStatus.DONE
                : ReminderChecklistStatus.IN_PROGRESS;

        return new ScheduleReminderChecklistItemResponse(
                "milestone-" + milestone.getId(),
                ReminderChecklistSourceType.MILESTONE,
                milestone.getId(),
                milestone.getTitle(),
                status,
                status.label(),
                null,
                null,
                milestone.getDueDate()
        );
    }

    public static ScheduleReminderChecklistItemResponse from(Task task) {
        ReminderChecklistStatus status = switch (task.getStatus()) {
            case DONE -> ReminderChecklistStatus.DONE;
            case HOLD -> ReminderChecklistStatus.BLOCKED;
            case TODO -> ReminderChecklistStatus.PENDING;
            case IN_PROGRESS, NEEDS_REVIEW -> ReminderChecklistStatus.IN_PROGRESS;
        };
        ProjectMember assignee = task.getAssignee();

        return new ScheduleReminderChecklistItemResponse(
                "task-" + task.getId(),
                ReminderChecklistSourceType.TASK,
                task.getId(),
                task.getTitle(),
                status,
                status.label(),
                assignee == null ? null : assignee.getMember().getNickname(),
                assignee == null ? null : assignee.getRole(),
                task.getDueDate()
        );
    }

    public static ScheduleReminderChecklistItemResponse from(Schedule schedule, boolean checked) {
        ReminderChecklistStatus status = checked ? ReminderChecklistStatus.DONE : ReminderChecklistStatus.PENDING;

        return new ScheduleReminderChecklistItemResponse(
                "schedule-" + schedule.getId(),
                ReminderChecklistSourceType.SCHEDULE,
                schedule.getId(),
                schedule.getTitle(),
                status,
                status.label(),
                schedule.getCreator().getNickname(),
                null,
                schedule.getEndAt().toLocalDate()
        );
    }

    public enum ReminderChecklistSourceType {
        MILESTONE,
        TASK,
        SCHEDULE,
        AI_UPDATE
    }

    public enum ReminderChecklistStatus {
        PENDING("대기"),
        IN_PROGRESS("진행 중"),
        DONE("완료"),
        BLOCKED("막힘");

        private final String label;

        ReminderChecklistStatus(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
