package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.enums.MilestoneStatus;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistSourceType;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistStatus;
import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleDetailResponse;
import com.wrap.domain.schedule.dto.ScheduleReminderResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.entity.ScheduleCheck;
import com.wrap.domain.schedule.enums.ScheduleType;
import com.wrap.domain.schedule.repository.ScheduleCheckRepository;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private ScheduleRepository scheduleRepository;
    private MemberRepository memberRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ScheduleCheckRepository scheduleCheckRepository;
    private MilestoneRepository milestoneRepository;
    private TaskRepository taskRepository;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepository.class);
        memberRepository = mock(MemberRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        scheduleCheckRepository = mock(ScheduleCheckRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        taskRepository = mock(TaskRepository.class);
        scheduleService = new ScheduleService(
                scheduleRepository,
                memberRepository,
                projectRepository,
                projectMemberRepository,
                scheduleCheckRepository,
                milestoneRepository,
                taskRepository
        );
    }

    @Test
    void createPrivateScheduleIgnoresProjectId() {
        Member creator = member(1L);
        ScheduleCreateRequest request = new ScheduleCreateRequest(
                10L,
                "Private schedule",
                "Project id should be ignored.",
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 1L);
            return schedule;
        });

        ScheduleResponse response = scheduleService.create(1L, request);

        assertThat(response.projectId()).isNull();
        assertThat(response.shared()).isFalse();
    }

    @Test
    void createScheduleIncludesTypeAndReminderFlag() {
        Member creator = member(1L);
        Project project = project(10L);
        ScheduleCreateRequest request = new ScheduleCreateRequest(
                10L,
                "Deadline schedule",
                "Use front calendar fields.",
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true,
                ScheduleType.DEADLINE,
                true
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 1L);
            return schedule;
        });

        ScheduleResponse response = scheduleService.create(1L, request);

        assertThat(response.type()).isEqualTo(ScheduleType.DEADLINE);
        assertThat(response.reminder()).isTrue();
    }

    @Test
    void updateScheduleAppliesOnlyProvidedFields() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "Old title",
                "Old description",
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                null,
                "New title",
                null,
                null,
                null,
                null
        );

        ScheduleResponse response = scheduleService.update(1L, 1L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("Old description");
        assertThat(response.startAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 14, 0));
        assertThat(response.endAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 15, 0));
        assertThat(response.shared()).isFalse();
    }

    @Test
    void updatePrivateScheduleRemovesProject() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                10L,
                null,
                null,
                null,
                null,
                false
        );

        ScheduleResponse response = scheduleService.update(1L, 1L, request);

        assertThat(response.projectId()).isNull();
        assertThat(response.shared()).isFalse();
    }

    @Test
    void projectOwnerCanDeleteSharedProjectSchedule() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(projectMemberRepository.existsByMemberIdAndProjectIdAndRoleAndStatus(
                2L,
                10L,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);

        scheduleService.delete(2L, 1L);

        verify(scheduleRepository).delete(schedule);
    }

    @Test
    void nonOwnerCannotDeleteAnotherMembersSchedule() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.delete(2L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void updateScheduleRejectsInvalidDateRangeAfterMerge() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "Old title",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                null,
                null,
                null,
                LocalDateTime.of(2026, 7, 23, 16, 0),
                null,
                null
        );

        assertThatThrownBy(() -> scheduleService.update(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    void remindersUseEndAtAsDeadline() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Deadline schedule",
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(2),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);
        when(scheduleRepository.findUpcomingSharedProjectSchedules(eq(10L), any(), any(), any()))
                .thenReturn(List.of(schedule));
        when(milestoneRepository.findByProjectIdAndDueDate(eq(10L), any()))
                .thenReturn(List.of());
        when(taskRepository.findReminderChecklistTasks(eq(10L), any()))
                .thenReturn(List.of());
        when(scheduleRepository.findSharedProjectSchedules(eq(10L), any(), any()))
                .thenReturn(List.of(schedule));

        List<ScheduleReminderResponse> responses = scheduleService.findReminders(1L, 10L, 7, 5);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).daysLeft()).isEqualTo(2);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(scheduleRepository).findUpcomingSharedProjectSchedules(
                eq(10L),
                nowCaptor.capture(),
                any(),
                any()
        );
        assertThat(schedule.getEndAt()).isAfter(nowCaptor.getValue());
    }

    @Test
    void remindersIncludeChecklistFromMilestonesTasksAndRelatedSchedules() {
        Member creator = member(1L);
        Project project = project(10L);
        LocalDate dueDate = LocalDate.now().plusDays(2);
        Schedule reminder = new Schedule(
                project,
                creator,
                "Release deadline",
                null,
                dueDate.atTime(10, 0),
                dueDate.atTime(11, 0),
                true,
                ScheduleType.DEADLINE,
                true
        );
        ReflectionTestUtils.setField(reminder, "id", 1L);
        Schedule relatedSchedule = new Schedule(
                project,
                creator,
                "Review meeting",
                null,
                dueDate.atTime(14, 0),
                dueDate.atTime(15, 0),
                true,
                ScheduleType.MEETING,
                false
        );
        ReflectionTestUtils.setField(relatedSchedule, "id", 2L);
        Milestone milestone = milestone(3L, project, dueDate);
        Task task = task(4L, project, projectMember(5L, creator, project), TaskStatus.HOLD, dueDate);

        when(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);
        when(scheduleRepository.findUpcomingSharedProjectSchedules(eq(10L), any(), any(), any()))
                .thenReturn(List.of(reminder));
        when(milestoneRepository.findByProjectIdAndDueDate(10L, dueDate))
                .thenReturn(List.of(milestone));
        when(taskRepository.findReminderChecklistTasks(10L, dueDate))
                .thenReturn(List.of(task));
        when(scheduleRepository.findSharedProjectSchedules(eq(10L), any(), any()))
                .thenReturn(List.of(reminder, relatedSchedule));
        when(scheduleCheckRepository.existsByScheduleIdAndMemberId(2L, 1L))
                .thenReturn(true);

        List<ScheduleReminderResponse> responses = scheduleService.findReminders(1L, 10L, 7, 5);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).checklist()).hasSize(3);
        assertThat(responses.get(0).checklist())
                .extracting(item -> item.sourceType())
                .containsExactly(
                        ReminderChecklistSourceType.MILESTONE,
                        ReminderChecklistSourceType.TASK,
                        ReminderChecklistSourceType.SCHEDULE
                );
        assertThat(responses.get(0).checklist().get(1).status())
                .isEqualTo(ReminderChecklistStatus.BLOCKED);
        assertThat(responses.get(0).checklist().get(2).status())
                .isEqualTo(ReminderChecklistStatus.DONE);
    }

    @Test
    void findDetailIncludesMyCheckedStatus() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "My schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleCheckRepository.existsByScheduleIdAndMemberId(1L, 1L)).thenReturn(true);

        ScheduleDetailResponse response = scheduleService.findDetail(1L, 1L);

        assertThat(response.checked()).isTrue();
        assertThat(response.title()).isEqualTo("My schedule");
    }

    @Test
    void otherMemberCannotReadPrivateSchedule() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "Private schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.findDetail(2L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void joinedProjectMemberCanReadSharedScheduleDetail() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                2L,
                10L,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);

        ScheduleDetailResponse response = scheduleService.findDetail(2L, 1L);

        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.shared()).isTrue();
    }

    @Test
    void checkScheduleStoresMyCheckOnce() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "My schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleCheckRepository.existsByScheduleIdAndMemberId(1L, 1L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(creator));

        ScheduleDetailResponse response = scheduleService.check(1L, 1L);

        assertThat(response.checked()).isTrue();
        verify(scheduleCheckRepository).save(any(ScheduleCheck.class));
    }

    @Test
    void uncheckScheduleDeletesMyCheckIfExists() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "My schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        ScheduleCheck scheduleCheck = ScheduleCheck.create(schedule, creator, LocalDateTime.now());
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleCheckRepository.findByScheduleIdAndMemberId(1L, 1L))
                .thenReturn(Optional.of(scheduleCheck));

        ScheduleDetailResponse response = scheduleService.uncheck(1L, 1L);

        assertThat(response.checked()).isFalse();
        verify(scheduleCheckRepository).delete(scheduleCheck);
    }

    private Member member(Long id) {
        Member member = instantiate(Member.class);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Project project(Long id) {
        Project project = instantiate(Project.class);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember projectMember(Long id, Member member, Project project) {
        ProjectMember projectMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(projectMember, "id", id);
        return projectMember;
    }

    private Milestone milestone(Long id, Project project, LocalDate dueDate) {
        Milestone milestone = instantiate(Milestone.class);
        ReflectionTestUtils.setField(milestone, "id", id);
        ReflectionTestUtils.setField(milestone, "project", project);
        ReflectionTestUtils.setField(milestone, "title", "Release milestone");
        ReflectionTestUtils.setField(milestone, "dueDate", dueDate);
        ReflectionTestUtils.setField(milestone, "status", MilestoneStatus.IN_PROGRESS);
        return milestone;
    }

    private Task task(Long id, Project project, ProjectMember assignee, TaskStatus status, LocalDate dueDate) {
        Task task = instantiate(Task.class);
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "assignee", assignee);
        ReflectionTestUtils.setField(task, "title", "Blocked task");
        ReflectionTestUtils.setField(task, "status", status);
        ReflectionTestUtils.setField(task, "dueDate", dueDate);
        ReflectionTestUtils.setField(task, "priority", TaskPriority.MEDIUM);
        return task;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity.", exception);
        }
    }
}
