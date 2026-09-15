package com.wrap.domain.task.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.dto.TaskResponse;
import com.wrap.domain.task.dto.TaskStatusUpdateRequest;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private ProjectMemberValidator projectMemberValidator;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectMemberValidator = mock(ProjectMemberValidator.class);
        taskService = new TaskService(taskRepository, projectMemberValidator);
    }

    @Test
    void findTasksReturnsFilteredProjectTasks() {
        Project project = project(10L);
        ProjectMember requester = projectMember(100L, 1L, project, ProjectMemberRole.MEMBER);
        Task task = task(1L, project, requester, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 9, 30));
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(taskRepository.findProjectTasks(
                10L,
                20L,
                TaskStatus.IN_PROGRESS,
                100L,
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        )).thenReturn(List.of(task));

        List<TaskResponse> responses = taskService.findTasks(
                1L,
                10L,
                20L,
                TaskStatus.IN_PROGRESS,
                100L,
                true,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "dueDate"
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).assignee().projectMemberId()).isEqualTo(100L);
        verify(projectMemberValidator).findJoinedMember(1L, 10L);
    }

    @Test
    void updateStatusChangesTaskStatus() {
        Project project = project(10L);
        ProjectMember requester = projectMember(100L, 1L, project, ProjectMemberRole.MEMBER);
        Task task = task(1L, project, requester, TaskStatus.IN_PROGRESS, LocalDate.of(2026, 9, 30));
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(taskRepository.findByIdAndProjectIdWithAssignee(1L, 10L))
                .thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateStatus(
                1L,
                10L,
                1L,
                new TaskStatusUpdateRequest(TaskStatus.DONE)
        );

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void deleteAllowsAssignee() {
        Project project = project(10L);
        ProjectMember assignee = projectMember(100L, 1L, project, ProjectMemberRole.MEMBER);
        Task task = task(1L, project, assignee, TaskStatus.TODO, LocalDate.of(2026, 9, 30));
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(assignee);
        when(taskRepository.findByIdAndProjectIdWithAssignee(1L, 10L))
                .thenReturn(Optional.of(task));

        taskService.delete(1L, 10L, 1L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteRejectsUnrelatedMember() {
        Project project = project(10L);
        ProjectMember requester = projectMember(200L, 2L, project, ProjectMemberRole.MEMBER);
        ProjectMember assignee = projectMember(100L, 1L, project, ProjectMemberRole.MEMBER);
        Task task = task(1L, project, assignee, TaskStatus.TODO, LocalDate.of(2026, 9, 30));
        when(projectMemberValidator.findJoinedMember(2L, 10L)).thenReturn(requester);
        when(taskRepository.findByIdAndProjectIdWithAssignee(1L, 10L))
                .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(2L, 10L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
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
        ReflectionTestUtils.setField(task, "title", "Task");
        ReflectionTestUtils.setField(task, "description", "Task description");
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
