package com.wrap.domain.schedule.service;

import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.schedule.dto.DeadlineSummaryResponse;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleCheckRepository;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeadlineSummaryService {

    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_LIMIT = 1;

    private final ProjectMemberValidator projectMemberValidator;
    private final ScheduleRepository scheduleRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final ScheduleCheckRepository scheduleCheckRepository;

    public List<DeadlineSummaryResponse> getSummaries(Long memberId, Long projectId, Integer limit) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
        if (resolvedLimit <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        return scheduleRepository.findUpcomingSharedProjectSchedules(
                        projectId,
                        now,
                        now.plusDays(DEFAULT_DAYS),
                        PageRequest.of(0, resolvedLimit)
                )
                .stream()
                .map(schedule -> DeadlineSummaryResponse.from(
                        schedule,
                        today,
                        findChecklist(memberId, schedule)
                ))
                .toList();
    }

    private List<ScheduleReminderChecklistItemResponse> findChecklist(Long memberId, Schedule schedule) {
        Long projectId = schedule.getProject().getId();
        LocalDate deadlineDate = schedule.getEndAt().toLocalDate();
        LocalDateTime dayStart = deadlineDate.atStartOfDay();
        LocalDateTime dayEnd = deadlineDate.plusDays(1).atStartOfDay();
        List<ScheduleReminderChecklistItemResponse> checklist = new ArrayList<>();

        milestoneRepository.findByProjectIdAndDueDate(projectId, deadlineDate)
                .stream()
                .map(ScheduleReminderChecklistItemResponse::from)
                .forEach(checklist::add);
        taskRepository.findReminderChecklistTasks(projectId, deadlineDate)
                .stream()
                .map(ScheduleReminderChecklistItemResponse::from)
                .forEach(checklist::add);
        scheduleRepository.findSharedProjectSchedules(projectId, dayStart, dayEnd)
                .stream()
                .filter(related -> !related.getId().equals(schedule.getId()))
                .map(related -> ScheduleReminderChecklistItemResponse.from(
                        related,
                        scheduleCheckRepository.existsByScheduleIdAndMemberId(related.getId(), memberId)
                ))
                .forEach(checklist::add);

        return checklist;
    }
}
