package com.wrap.domain.calendar.service;

import com.wrap.domain.calendar.dto.CalendarRiskCheckResponse;
import com.wrap.domain.calendar.enums.CalendarRiskLevel;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    private TaskRepository taskRepository;
    private ProjectMemberValidator projectMemberValidator;
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectMemberValidator = mock(ProjectMemberValidator.class);
        calendarService = new CalendarService(taskRepository, projectMemberValidator);
    }

    @Test
    void findRiskChecksReturnsTaskRiskSignals() {
        Project project = project(10L);
        ProjectMember assignee = projectMember(20L, 2L, project);
        Task task = task(1L, project, assignee, TaskStatus.HOLD, LocalDate.now().plusDays(1));

        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(assignee);
        when(taskRepository.findCalendarRiskChecks(
                10L,
                TaskStatus.HOLD,
                20L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                TaskStatus.DONE
        )).thenReturn(List.of(task));

        List<CalendarRiskCheckResponse> responses = calendarService.findRiskChecks(
                1L,
                10L,
                TaskStatus.HOLD,
                20L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).taskId()).isEqualTo(1L);
        assertThat(responses.get(0).assigneeProjectMemberId()).isEqualTo(20L);
        assertThat(responses.get(0).riskLevel()).isEqualTo(CalendarRiskLevel.BLOCKED);
        verify(projectMemberValidator).findJoinedMember(1L, 10L);
    }

    private Task task(Long id, Project project, ProjectMember assignee, TaskStatus status, LocalDate dueDate) {
        Task task = new TaskFixture().task();
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "assignee", assignee);
        ReflectionTestUtils.setField(task, "title", "Blocked task");
        ReflectionTestUtils.setField(task, "description", "Waiting for data.");
        ReflectionTestUtils.setField(task, "status", status);
        ReflectionTestUtils.setField(task, "dueDate", dueDate);
        ReflectionTestUtils.setField(task, "priority", TaskPriority.MEDIUM);
        return task;
    }

    private Project project(Long id) {
        Project project = Project.create("Project", null, null, null, null, null, Project.DEFAULT_COLOR);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember projectMember(Long id, Long memberId, Project project) {
        Member member = Member.builder()
                .email("member" + memberId + "@wrap.com")
                .password("password")
                .nickname("member" + memberId)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        ProjectMember projectMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.now()
        );
        ReflectionTestUtils.setField(projectMember, "id", id);
        return projectMember;
    }

    private static class TaskFixture {

        private Task task() {
            try {
                var constructor = Task.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to instantiate test entity.", exception);
            }
        }
    }
}
