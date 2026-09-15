package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.enums.MilestoneStatus;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.schedule.dto.ReminderItemStatusResponse;
import com.wrap.domain.schedule.dto.ReminderItemStatusUpdateRequest;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistSourceType;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistStatus;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.entity.ScheduleCheck;
import com.wrap.domain.schedule.repository.ScheduleCheckRepository;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleReminderItemService {

    private final ProjectMemberValidator projectMemberValidator;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleCheckRepository scheduleCheckRepository;
    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;

    @Transactional
    public ReminderItemStatusResponse updateStatus(
            Long memberId,
            Long projectId,
            Long scheduleId,
            ReminderChecklistSourceType sourceType,
            Long sourceId,
            ReminderItemStatusUpdateRequest request
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        findProjectSchedule(projectId, scheduleId);

        return switch (sourceType) {
            case TASK -> updateTaskStatus(projectId, sourceId, request.status());
            case SCHEDULE -> updateScheduleCheck(memberId, projectId, sourceId, request.status());
            case MILESTONE -> updateMilestoneStatus(projectId, sourceId, request.status());
            case AI_UPDATE -> throw new CustomException(ErrorCode.INVALID_REQUEST);
        };
    }

    private ReminderItemStatusResponse updateTaskStatus(
            Long projectId,
            Long taskId,
            ReminderChecklistStatus status
    ) {
        Task task = taskRepository.findByIdAndProjectIdWithAssignee(taskId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
        task.updateStatus(toTaskStatus(status));
        return ReminderItemStatusResponse.of(ReminderChecklistSourceType.TASK, taskId, status);
    }

    private ReminderItemStatusResponse updateScheduleCheck(
            Long memberId,
            Long projectId,
            Long relatedScheduleId,
            ReminderChecklistStatus status
    ) {
        Schedule relatedSchedule = findProjectSchedule(projectId, relatedScheduleId);
        if (status == ReminderChecklistStatus.BLOCKED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (status == ReminderChecklistStatus.DONE) {
            if (!scheduleCheckRepository.existsByScheduleIdAndMemberId(relatedScheduleId, memberId)) {
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
                scheduleCheckRepository.save(ScheduleCheck.create(
                        relatedSchedule,
                        member,
                        java.time.LocalDateTime.now()
                ));
            }
        } else {
            scheduleCheckRepository.findByScheduleIdAndMemberId(relatedScheduleId, memberId)
                    .ifPresent(scheduleCheckRepository::delete);
        }

        ReminderChecklistStatus resolved = status == ReminderChecklistStatus.DONE
                ? ReminderChecklistStatus.DONE
                : ReminderChecklistStatus.PENDING;
        return ReminderItemStatusResponse.of(
                ReminderChecklistSourceType.SCHEDULE,
                relatedScheduleId,
                resolved
        );
    }

    private ReminderItemStatusResponse updateMilestoneStatus(
            Long projectId,
            Long milestoneId,
            ReminderChecklistStatus status
    ) {
        if (status == ReminderChecklistStatus.BLOCKED) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        Milestone milestone = milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
        ReminderChecklistStatus resolved = status == ReminderChecklistStatus.DONE
                ? ReminderChecklistStatus.DONE
                : ReminderChecklistStatus.IN_PROGRESS;
        milestone.updateStatus(resolved == ReminderChecklistStatus.DONE
                ? MilestoneStatus.DONE
                : MilestoneStatus.IN_PROGRESS);
        return ReminderItemStatusResponse.of(
                ReminderChecklistSourceType.MILESTONE,
                milestoneId,
                resolved
        );
    }

    private Schedule findProjectSchedule(Long projectId, Long scheduleId) {
        return scheduleRepository.findByIdAndProjectId(scheduleId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private TaskStatus toTaskStatus(ReminderChecklistStatus status) {
        return switch (status) {
            case PENDING -> TaskStatus.TODO;
            case IN_PROGRESS -> TaskStatus.IN_PROGRESS;
            case DONE -> TaskStatus.DONE;
            case BLOCKED -> TaskStatus.HOLD;
        };
    }
}
