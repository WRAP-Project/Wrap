package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
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
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleReminderItemServiceTest {

    private ProjectMemberValidator projectMemberValidator;
    private MemberRepository memberRepository;
    private ScheduleRepository scheduleRepository;
    private ScheduleCheckRepository scheduleCheckRepository;
    private TaskRepository taskRepository;
    private MilestoneRepository milestoneRepository;
    private ScheduleReminderItemService service;

    @BeforeEach
    void setUp() {
        projectMemberValidator = mock(ProjectMemberValidator.class);
        memberRepository = mock(MemberRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        scheduleCheckRepository = mock(ScheduleCheckRepository.class);
        taskRepository = mock(TaskRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        service = new ScheduleReminderItemService(
                projectMemberValidator,
                memberRepository,
                scheduleRepository,
                scheduleCheckRepository,
                taskRepository,
                milestoneRepository
        );
    }

    @Test
    void updateTaskReminderItemStatusStoresTaskStatus() {
        Project project = project(10L);
        Member member = member(1L);
        ProjectMember requester = projectMember(100L, member, project);
        Schedule schedule = schedule(1L, project, member);
        Task task = task(2L, project, requester, TaskStatus.IN_PROGRESS);
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(scheduleRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(schedule));
        when(taskRepository.findByIdAndProjectIdWithAssignee(2L, 10L)).thenReturn(Optional.of(task));

        ReminderItemStatusResponse response = service.updateStatus(
                1L,
                10L,
                1L,
                ReminderChecklistSourceType.TASK,
                2L,
                new ReminderItemStatusUpdateRequest(ReminderChecklistStatus.BLOCKED)
        );

        assertThat(response.status()).isEqualTo(ReminderChecklistStatus.BLOCKED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.HOLD);
    }

    @Test
    void updateScheduleReminderItemDoneCreatesScheduleCheck() {
        Project project = project(10L);
        Member member = member(1L);
        ProjectMember requester = projectMember(100L, member, project);
        Schedule schedule = schedule(1L, project, member);
        Schedule relatedSchedule = schedule(2L, project, member);
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(scheduleRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.findByIdAndProjectId(2L, 10L)).thenReturn(Optional.of(relatedSchedule));
        when(scheduleCheckRepository.existsByScheduleIdAndMemberId(2L, 1L)).thenReturn(false);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        ReminderItemStatusResponse response = service.updateStatus(
                1L,
                10L,
                1L,
                ReminderChecklistSourceType.SCHEDULE,
                2L,
                new ReminderItemStatusUpdateRequest(ReminderChecklistStatus.DONE)
        );

        assertThat(response.status()).isEqualTo(ReminderChecklistStatus.DONE);
        verify(scheduleCheckRepository).save(any(ScheduleCheck.class));
    }

    private Project project(Long id) {
        Project project = Project.create("Project", null, null, null, null, null, Project.DEFAULT_COLOR);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private Member member(Long id) {
        Member member = Member.builder()
                .email("member" + id + "@wrap.com")
                .password("password")
                .nickname("member" + id)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
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

    private Schedule schedule(Long id, Project project, Member creator) {
        Schedule schedule = new Schedule(
                project,
                creator,
                "Schedule",
                null,
                LocalDateTime.of(2026, 9, 30, 10, 0),
                LocalDateTime.of(2026, 9, 30, 11, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private Task task(Long id, Project project, ProjectMember assignee, TaskStatus status) {
        Task task = instantiate(Task.class);
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "assignee", assignee);
        ReflectionTestUtils.setField(task, "title", "Task");
        ReflectionTestUtils.setField(task, "status", status);
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
