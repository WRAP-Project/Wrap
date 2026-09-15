package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.schedule.dto.DeadlineSummaryResponse;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleCheckRepository;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeadlineSummaryServiceTest {

    private ProjectMemberValidator projectMemberValidator;
    private ScheduleRepository scheduleRepository;
    private MilestoneRepository milestoneRepository;
    private TaskRepository taskRepository;
    private ScheduleCheckRepository scheduleCheckRepository;
    private DeadlineSummaryService deadlineSummaryService;

    @BeforeEach
    void setUp() {
        projectMemberValidator = mock(ProjectMemberValidator.class);
        scheduleRepository = mock(ScheduleRepository.class);
        milestoneRepository = mock(MilestoneRepository.class);
        taskRepository = mock(TaskRepository.class);
        scheduleCheckRepository = mock(ScheduleCheckRepository.class);
        deadlineSummaryService = new DeadlineSummaryService(
                projectMemberValidator,
                scheduleRepository,
                milestoneRepository,
                taskRepository,
                scheduleCheckRepository
        );
    }

    @Test
    void getSummariesReturnsUpcomingDeadlineChecklist() {
        Project project = project(10L);
        Member member = member(1L);
        ProjectMember requester = projectMember(100L, member, project);
        Schedule deadline = schedule(1L, project, member, LocalDate.now().plusDays(2));
        Task task = task(2L, project, requester, TaskStatus.DONE, deadline.getEndAt().toLocalDate());
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(scheduleRepository.findUpcomingSharedProjectSchedules(eq(10L), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(deadline));
        when(milestoneRepository.findByProjectIdAndDueDate(eq(10L), any())).thenReturn(List.of());
        when(taskRepository.findReminderChecklistTasks(10L, deadline.getEndAt().toLocalDate()))
                .thenReturn(List.of(task));
        when(scheduleRepository.findSharedProjectSchedules(eq(10L), any(), any()))
                .thenReturn(List.of(deadline));

        List<DeadlineSummaryResponse> responses = deadlineSummaryService.getSummaries(1L, 10L, 1);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).header().scheduleId()).isEqualTo(1L);
        assertThat(responses.get(0).stats().checklistDone()).isEqualTo(1);
        assertThat(responses.get(0).checklist()).hasSize(1);
        verify(projectMemberValidator).findJoinedMember(1L, 10L);
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

    private Schedule schedule(Long id, Project project, Member creator, LocalDate dueDate) {
        Schedule schedule = new Schedule(
                project,
                creator,
                "Deadline",
                null,
                dueDate.atTime(10, 0),
                dueDate.atTime(11, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", id);
        return schedule;
    }

    private Task task(Long id, Project project, ProjectMember assignee, TaskStatus status, LocalDate dueDate) {
        Task task = instantiate(Task.class);
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "assignee", assignee);
        ReflectionTestUtils.setField(task, "title", "Task");
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
