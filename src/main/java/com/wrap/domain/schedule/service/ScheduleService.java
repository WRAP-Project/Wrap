package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleDetailResponse;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse;
import com.wrap.domain.schedule.dto.ScheduleReminderResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.entity.ScheduleCheck;
import com.wrap.domain.schedule.enums.ScheduleType;
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
public class ScheduleService {

    private static final int DEFAULT_REMINDER_DAYS = 7;
    private static final int DEFAULT_REMINDER_LIMIT = 5;

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ScheduleCheckRepository scheduleCheckRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public ScheduleResponse create(Long memberId, ScheduleCreateRequest request) {
        Member creator = findMember(memberId);
        Project project = resolveProject(memberId, request.projectId(), request.shared());

        Schedule schedule = new Schedule(
                project,
                creator,
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.shared(),
                resolveType(request.type()),
                Boolean.TRUE.equals(request.reminder())
        );

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public List<ScheduleResponse> findMine(Long memberId, LocalDate from, LocalDate to, Long projectId) {
        if (projectId != null) {
            validateJoinedProjectMember(memberId, projectId);
        } else {
            findMember(memberId);
        }

        return scheduleRepository.findMySchedules(
                        memberId,
                        projectId,
                        toStartOfDay(from),
                        toExclusiveEnd(to)
                )
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public List<ScheduleResponse> findProjectSchedules(Long memberId, Long projectId, LocalDate from, LocalDate to) {
        validateJoinedProjectMember(memberId, projectId);

        return scheduleRepository.findSharedProjectSchedules(
                        projectId,
                        toStartOfDay(from),
                        toExclusiveEnd(to)
                )
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public ScheduleDetailResponse findDetail(Long memberId, Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        validateScheduleReadable(memberId, schedule);

        return ScheduleDetailResponse.from(schedule, isChecked(scheduleId, memberId));
    }

    @Transactional
    public ScheduleResponse update(Long memberId, Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = findSchedule(scheduleId);
        validateScheduleWritable(memberId, schedule);

        Project project = resolveProjectForUpdate(memberId, schedule, request);
        String title = request.title() == null ? schedule.getTitle() : request.title();
        String description = request.description() == null ? schedule.getDescription() : request.description();
        LocalDateTime startAt = request.startAt() == null ? schedule.getStartAt() : request.startAt();
        LocalDateTime endAt = request.endAt() == null ? schedule.getEndAt() : request.endAt();
        boolean shared = request.shared() == null ? schedule.isShared() : request.shared();
        ScheduleType type = request.type() == null ? schedule.getType() : request.type();
        boolean reminder = request.reminder() == null
                ? schedule.isReminder()
                : request.reminder();

        validateDateRange(startAt, endAt);
        schedule.update(
                project,
                title,
                description,
                startAt,
                endAt,
                shared,
                type,
                reminder
        );

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long memberId, Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        validateScheduleWritable(memberId, schedule);
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public ScheduleDetailResponse check(Long memberId, Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        validateScheduleReadable(memberId, schedule);

        if (!isChecked(scheduleId, memberId)) {
            Member member = findMember(memberId);
            scheduleCheckRepository.save(ScheduleCheck.create(schedule, member, LocalDateTime.now()));
        }

        return ScheduleDetailResponse.from(schedule, true);
    }

    @Transactional
    public ScheduleDetailResponse uncheck(Long memberId, Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        validateScheduleReadable(memberId, schedule);

        scheduleCheckRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .ifPresent(scheduleCheckRepository::delete);

        return ScheduleDetailResponse.from(schedule, false);
    }

    public List<ScheduleReminderResponse> findReminders(Long memberId, Long projectId, Integer days, Integer limit) {
        validateJoinedProjectMember(memberId, projectId);

        int reminderDays = days == null ? DEFAULT_REMINDER_DAYS : days;
        int reminderLimit = limit == null ? DEFAULT_REMINDER_LIMIT : limit;
        if (reminderDays <= 0 || reminderLimit <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(reminderDays);
        LocalDate today = now.toLocalDate();

        return scheduleRepository.findUpcomingSharedProjectSchedules(
                        projectId,
                        now,
                        until,
                        PageRequest.of(0, reminderLimit)
                )
                .stream()
                .map(schedule -> ScheduleReminderResponse.from(
                        schedule,
                        today,
                        findReminderChecklist(memberId, schedule)
                ))
                .toList();
    }

    private List<ScheduleReminderChecklistItemResponse> findReminderChecklist(Long memberId, Schedule schedule) {
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
                .map(related -> ScheduleReminderChecklistItemResponse.from(related, isChecked(related.getId(), memberId)))
                .forEach(checklist::add);

        return checklist;
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private Project resolveProject(Long memberId, Long projectId, boolean shared) {
        if (shared && projectId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        if (!shared) {
            return null;
        }

        if (projectId == null) {
            return null;
        }

        validateJoinedProjectMember(memberId, projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Project resolveProjectForUpdate(Long memberId, Schedule schedule, ScheduleUpdateRequest request) {
        boolean shared = request.shared() == null ? schedule.isShared() : request.shared();

        if (!shared) {
            return null;
        }

        Long projectId = request.projectId();
        if (projectId == null) {
            if (schedule.getProject() == null) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            projectId = schedule.getProject().getId();
        }

        validateJoinedProjectMember(memberId, projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private void validateJoinedProjectMember(Long memberId, Long projectId) {
        boolean exists = projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                memberId,
                projectId,
                ProjectMemberStatus.JOINED
        );

        if (!exists) {
            if (!projectRepository.existsById(projectId)) {
                throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateScheduleReadable(Long memberId, Schedule schedule) {
        if (schedule.getCreator().getId().equals(memberId)) {
            return;
        }

        Project project = schedule.getProject();
        if (schedule.isShared() && project != null) {
            validateJoinedProjectMember(memberId, project.getId());
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private void validateScheduleWritable(Long memberId, Schedule schedule) {
        if (schedule.getCreator().getId().equals(memberId)) {
            return;
        }

        Project project = schedule.getProject();
        if (schedule.isShared() && project != null && isProjectOwner(memberId, project.getId())) {
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private boolean isChecked(Long scheduleId, Long memberId) {
        return scheduleCheckRepository.existsByScheduleIdAndMemberId(scheduleId, memberId);
    }

    private boolean isProjectOwner(Long memberId, Long projectId) {
        return projectMemberRepository.existsByMemberIdAndProjectIdAndRoleAndStatus(
                memberId,
                projectId,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        );
    }

    private void validateDateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private ScheduleType resolveType(ScheduleType type) {
        return type == null ? ScheduleType.MEETING : type;
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toExclusiveEnd(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }
}
