package com.wrap.domain.milestone.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.milestone.dto.request.MilestoneCreateRequest;
import com.wrap.domain.milestone.dto.request.MilestoneUpdateRequest;
import com.wrap.domain.milestone.dto.response.MilestoneResponse;
import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MilestoneServiceTest {

    private MilestoneRepository milestoneRepository;
    private TaskRepository taskRepository;
    private ProjectMemberValidator projectMemberValidator;
    private MilestoneService milestoneService;

    @BeforeEach
    void setUp() {
        milestoneRepository = mock(MilestoneRepository.class);
        taskRepository = mock(TaskRepository.class);
        projectMemberValidator = mock(ProjectMemberValidator.class);
        milestoneService = new MilestoneService(
                milestoneRepository,
                taskRepository,
                projectMemberValidator
        );
    }

    @Test
    void createStoresMilestoneWithTitleAndDueDate() {
        Project project = project(10L);
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(projectMember(project));
        when(milestoneRepository.save(any(Milestone.class))).thenAnswer(invocation -> {
            Milestone milestone = invocation.getArgument(0);
            ReflectionTestUtils.setField(milestone, "id", 3L);
            return milestone;
        });

        MilestoneResponse response = milestoneService.create(
                1L,
                10L,
                new MilestoneCreateRequest("중간 발표 자료 완성", LocalDate.of(2026, 8, 20))
        );

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("중간 발표 자료 완성");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void findProjectMilestonesReturnsMilestonesOrderedByDueDate() {
        Project project = project(10L);
        when(milestoneRepository.findAllByProjectIdOrderByDueDateAscIdAsc(10L))
                .thenReturn(List.of(
                        milestone(3L, project, LocalDate.of(2026, 8, 20)),
                        milestone(4L, project, LocalDate.of(2026, 9, 1))
                ));

        List<MilestoneResponse> responses = milestoneService.findProjectMilestones(1L, 10L);

        assertThat(responses).extracting(MilestoneResponse::id).containsExactly(3L, 4L);
        verify(projectMemberValidator).findJoinedMember(1L, 10L);
    }

    @Test
    void updateAppliesOnlyProvidedFields() {
        Project project = project(10L);
        Milestone milestone = milestone(3L, project, LocalDate.of(2026, 8, 20));
        when(milestoneRepository.findByIdAndProjectId(3L, 10L)).thenReturn(Optional.of(milestone));

        MilestoneResponse response = milestoneService.update(
                1L,
                10L,
                3L,
                new MilestoneUpdateRequest("최종 발표 자료 완성", null, null)
        );

        assertThat(response.title()).isEqualTo("최종 발표 자료 완성");
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void deleteUnlinksTasksInsteadOfDeletingThem() {
        Project project = project(10L);
        Milestone milestone = milestone(3L, project, LocalDate.of(2026, 8, 20));
        when(milestoneRepository.findByIdAndProjectId(3L, 10L)).thenReturn(Optional.of(milestone));

        milestoneService.delete(1L, 10L, 3L);

        verify(taskRepository).clearMilestone(3L);
        verify(milestoneRepository).delete(milestone);
    }

    @Test
    void updateRejectsMilestoneOfAnotherProject() {
        when(milestoneRepository.findByIdAndProjectId(3L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> milestoneService.update(
                1L,
                10L,
                3L,
                new MilestoneUpdateRequest(null, null, null)
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MILESTONE_NOT_FOUND));
    }

    private Project project(Long id) {
        Project project = instantiate(Project.class);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember projectMember(Project project) {
        Member member = instantiate(Member.class);
        ReflectionTestUtils.setField(member, "id", 1L);
        return ProjectMember.join(member, project, ProjectMemberRole.MEMBER, LocalDateTime.now());
    }

    private Milestone milestone(Long id, Project project, LocalDate dueDate) {
        Milestone milestone = Milestone.create(project, "중간 발표 자료 완성", null, dueDate);
        ReflectionTestUtils.setField(milestone, "id", id);
        return milestone;
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
