package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleReminderResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.global.exception.BusinessException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                request.shared()
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

        validateDateRange(startAt, endAt);
        schedule.update(
                project,
                title,
                description,
                startAt,
                endAt,
                shared
        );

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long memberId, Long scheduleId) {
        Schedule schedule = findSchedule(scheduleId);
        validateScheduleWritable(memberId, schedule);
        scheduleRepository.delete(schedule);
    }

    public List<ScheduleReminderResponse> findReminders(Long memberId, Long projectId, Integer days, Integer limit) {
        validateJoinedProjectMember(memberId, projectId);

        int reminderDays = days == null ? DEFAULT_REMINDER_DAYS : days;
        int reminderLimit = limit == null ? DEFAULT_REMINDER_LIMIT : limit;
        if (reminderDays <= 0 || reminderLimit <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
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
                .map(schedule -> ScheduleReminderResponse.from(schedule, today))
                .toList();
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private Schedule findSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private Project resolveProject(Long memberId, Long projectId, boolean shared) {
        if (shared && projectId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (projectId == null) {
            return null;
        }

        validateJoinedProjectMember(memberId, projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Project resolveProjectForUpdate(Long memberId, Schedule schedule, ScheduleUpdateRequest request) {
        boolean shared = request.shared() == null ? schedule.isShared() : request.shared();

        if (!shared) {
            return null;
        }

        Long projectId = request.projectId();
        if (projectId == null) {
            if (schedule.getProject() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            projectId = schedule.getProject().getId();
        }

        validateJoinedProjectMember(memberId, projectId);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private void validateJoinedProjectMember(Long memberId, Long projectId) {
        boolean exists = projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                memberId,
                projectId,
                ProjectMemberStatus.JOINED
        );

        if (!exists) {
            if (!projectRepository.existsById(projectId)) {
                throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateScheduleWritable(Long memberId, Schedule schedule) {
        if (schedule.getCreator().getId().equals(memberId)) {
            return;
        }

        Project project = schedule.getProject();
        if (schedule.isShared() && project != null && isProjectOwner(memberId, project.getId())) {
            return;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
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
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toExclusiveEnd(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }
}
