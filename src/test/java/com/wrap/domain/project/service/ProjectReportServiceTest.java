package com.wrap.domain.project.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.dto.response.ProjectReportResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectReportAreaType;
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
        requester.changeWorkRole("개발");
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
        assertThat(response.areas().get(0).area()).isEqualTo("개발");
        assertThat(response.areas().get(0).areaType())
                .isEqualTo(ProjectReportAreaType.WORK_ROLE);
        assertThat(response.areas().get(0).percent()).isEqualTo(33);
        assertThat(response.areas().get(0).delayed()).isTrue();
        assertThat(response.risks()).hasSize(1);
        assertThat(response.risks().get(0).taskId()).isEqualTo(3L);
        verify(projectMemberValidator).findJoinedMember(1L, 10L);
    }

    @Test
    void getReportGroupsTasksByCurrentWorkRole() {
        Project project = project(10L);
        ProjectMember requester = projectMember(100L, 1L, project, ProjectMemberRole.OWNER);
        ProjectMember firstDesigner = projectMember(
                200L, 2L, project, ProjectMemberRole.MEMBER);
        ProjectMember secondDesigner = projectMember(
                300L, 3L, project, ProjectMemberRole.OWNER);
        ProjectMember developer = projectMember(
                400L, 4L, project, ProjectMemberRole.MEMBER);
        firstDesigner.changeWorkRole("디자인");
        secondDesigner.changeWorkRole("디자인");
        developer.changeWorkRole("개발");
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(taskRepository.findProjectReportTasks(10L)).thenReturn(List.of(
                task(1L, project, firstDesigner, TaskStatus.DONE,
                        LocalDate.now().plusDays(10)),
                task(2L, project, secondDesigner, TaskStatus.IN_PROGRESS,
                        LocalDate.now().plusDays(10)),
                task(3L, project, developer, TaskStatus.HOLD,
                        LocalDate.now().plusDays(10))
        ));

        ProjectReportResponse response = projectReportService.getReport(1L, 10L);

        assertThat(response.areas()).hasSize(2);
        assertThat(response.areas().get(0).area()).isEqualTo("개발");
        assertThat(response.areas().get(0).areaType())
                .isEqualTo(ProjectReportAreaType.WORK_ROLE);
        assertThat(response.areas().get(0).percent()).isZero();
        assertThat(response.areas().get(0).delayed()).isTrue();
        assertThat(response.areas().get(0).note()).isEqualTo("확인 필요");
        assertThat(response.areas().get(1).area()).isEqualTo("디자인");
        assertThat(response.areas().get(1).areaType())
                .isEqualTo(ProjectReportAreaType.WORK_ROLE);
        assertThat(response.areas().get(1).percent()).isEqualTo(50);
        assertThat(response.areas().get(1).delayed()).isFalse();
        assertThat(response.areas().get(1).note()).isNull();
    }

    @Test
    void getReportSeparatesSystemAreasFromIdenticallyNamedWorkRoles() {
        Project project = project(10L);
        ProjectMember requester = projectMember(100L, 1L, project, ProjectMemberRole.OWNER);
        ProjectMember workRoleUnassigned = projectMember(
                200L, 2L, project, ProjectMemberRole.MEMBER);
        ProjectMember unspecified = projectMember(
                300L, 3L, project, ProjectMemberRole.MEMBER);
        ProjectMember workRoleUnspecified = projectMember(
                400L, 4L, project, ProjectMemberRole.MEMBER);
        workRoleUnassigned.changeWorkRole("UNASSIGNED");
        workRoleUnspecified.changeWorkRole("UNSPECIFIED");
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(taskRepository.findProjectReportTasks(10L)).thenReturn(List.of(
                task(1L, project, null, TaskStatus.DONE, null),
                task(2L, project, workRoleUnassigned, TaskStatus.IN_PROGRESS, null),
                task(3L, project, unspecified, TaskStatus.DONE, null),
                task(4L, project, workRoleUnspecified, TaskStatus.IN_PROGRESS, null)
        ));

        ProjectReportResponse response = projectReportService.getReport(1L, 10L);

        assertThat(response.areas()).hasSize(4);
        assertArea(response, 0, "UNASSIGNED", ProjectReportAreaType.WORK_ROLE, 0);
        assertArea(response, 1, "UNASSIGNED", ProjectReportAreaType.UNASSIGNED, 100);
        assertArea(response, 2, "UNSPECIFIED", ProjectReportAreaType.WORK_ROLE, 0);
        assertArea(response, 3, "UNSPECIFIED", ProjectReportAreaType.UNSPECIFIED, 100);
    }

    @Test
    void getReportUsesChangedWorkRoleForExistingTasks() {
        Project project = project(10L);
        ProjectMember requester = projectMember(100L, 1L, project, ProjectMemberRole.OWNER);
        ProjectMember assignee = projectMember(
                200L, 2L, project, ProjectMemberRole.MEMBER);
        assignee.changeWorkRole("디자인");
        List<Task> tasks = List.of(
                task(1L, project, assignee, TaskStatus.DONE, null)
        );
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(requester);
        when(taskRepository.findProjectReportTasks(10L)).thenReturn(tasks);

        ProjectReportResponse beforeChange = projectReportService.getReport(1L, 10L);
        assignee.changeWorkRole("개발");
        ProjectReportResponse afterChange = projectReportService.getReport(1L, 10L);

        assertThat(beforeChange.areas()).singleElement()
                .satisfies(area -> {
                    assertThat(area.area()).isEqualTo("디자인");
                    assertThat(area.areaType()).isEqualTo(ProjectReportAreaType.WORK_ROLE);
                });
        assertThat(afterChange.areas()).singleElement()
                .satisfies(area -> {
                    assertThat(area.area()).isEqualTo("개발");
                    assertThat(area.areaType()).isEqualTo(ProjectReportAreaType.WORK_ROLE);
                });
    }

    private void assertArea(
            ProjectReportResponse response,
            int index,
            String area,
            ProjectReportAreaType areaType,
            int percent
    ) {
        assertThat(response.areas().get(index).area()).isEqualTo(area);
        assertThat(response.areas().get(index).areaType()).isEqualTo(areaType);
        assertThat(response.areas().get(index).percent()).isEqualTo(percent);
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
