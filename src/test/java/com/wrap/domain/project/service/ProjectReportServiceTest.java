package com.wrap.domain.project.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.dto.response.ProjectReportResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectReportServiceTest {

    private ProjectMemberValidator projectMemberValidator;
    private TaskRepository taskRepository;
    private ProjectReportService projectReportService;

    @BeforeEach
    void setUp() {
        projectMemberValidator = mock(ProjectMemberValidator.class);
        taskRepository = mock(TaskRepository.class);
        projectReportService = new ProjectReportService(projectMemberValidator, taskRepository);
    }

    @Test
    void getReportBuildsProgressAndRiskSummary() {
        Project project = project(10L);
        ProjectMember requester = projectMember(100L, 1L, project, ProjectMemberRole.MEMBER);
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(taskRepository.findProjectReportTasks(10L)).thenReturn(List.of(
                task(1L, project, requester, TaskStatus.DONE, LocalDate.now().minusDays(1)),
                task(2L, project, requester, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(10)),
                task(3L, project, requester, TaskStatus.HOLD, LocalDate.now().plusDays(1))
        ));

        ProjectReportResponse response = projectReportService.getReport(1L, 10L);

        assertThat(response.percent()).isEqualTo(33);
        assertThat(response.doneCount()).isEqualTo(1);
        assertThat(response.inProgressCount()).isEqualTo(1);
        assertThat(response.needsCheckCount()).isEqualTo(1);
        assertThat(response.areas()).hasSize(1);
        assertThat(response.risks()).hasSize(1);
        assertThat(response.risks().get(0).taskId()).isEqualTo(3L);
        verify(projectMemberValidator).findJoinedMember(1L, 10L);
    }

    private Project project(Long id) {
        Project project = Project.create("Project", null, null, null, null, null, Project.DEFAULT_COLOR);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember projectMember(
            Long id,
            Long memberId,
            Project project,
            ProjectMemberRole role
    ) {
        Member member = Member.builder()
                .email("member" + memberId + "@wrap.com")
                .password("password")
                .nickname("member" + memberId)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        ProjectMember projectMember = ProjectMember.join(member, project, role, LocalDateTime.now());
        ReflectionTestUtils.setField(projectMember, "id", id);
        return projectMember;
    }

    private Task task(
            Long id,
            Project project,
            ProjectMember assignee,
            TaskStatus status,
            LocalDate dueDate
    ) {
        Task task = instantiate(Task.class);
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "assignee", assignee);
        ReflectionTestUtils.setField(task, "title", "Task " + id);
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
