package com.wrap.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.request.ProjectUpdateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.dto.response.ProjectSummaryResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("프로젝트 생성 성공 - 생성자를 OWNER로 등록한다")
    void create_success() {
        Member member = mock(Member.class);
        ProjectCreateRequest request = createRequest(
                "Wrap",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(projectRepository.save(any(Project.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.create(1L, request);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getName()).isEqualTo("Wrap");
        assertThat(response.getColor()).isEqualTo("#A78BFA");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        ArgumentCaptor<ProjectMember> ownerCaptor =
                ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(ownerCaptor.capture());

        ProjectMember owner = ownerCaptor.getValue();
        assertThat(owner.getMember()).isSameAs(member);
        assertThat(owner.getProject().getName()).isEqualTo("Wrap");
        assertThat(owner.getProject().getColor()).isEqualTo("#A78BFA");
        assertThat(owner.isJoinedOwner()).isTrue();
        assertThat(owner.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 회원을 찾을 수 없다")
    void create_memberNotFound() {
        ProjectCreateRequest request = createRequest(
                "Wrap",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

        verifyNoInteractions(projectRepository, projectMemberRepository);
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 시작일이 종료일보다 늦다")
    void create_invalidDateRange() {
        Member member = mock(Member.class);
        ProjectCreateRequest request = createRequest(
                "Wrap",
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 7, 1)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> projectService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.INVALID_DATE_RANGE));

        verifyNoInteractions(projectRepository, projectMemberRepository);
    }

    @Test
    @DisplayName("내 프로젝트 목록 조회 성공 - 참여 중인 프로젝트를 요약 응답으로 반환한다")
    void getMyProjects_success() {
        Member member = mock(Member.class);
        Project wrap = project(10L, "Wrap");
        Project graduation = project(20L, "Graduation");
        graduation.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        LocalDateTime joinedAt = LocalDateTime.of(2026, 7, 1, 9, 0);
        List<ProjectMember> memberships = List.of(
                ProjectMember.createOwner(member, wrap, joinedAt),
                ProjectMember.createOwner(member, graduation, joinedAt)
        );
        given(projectMemberRepository
                .findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
                        1L,
                        ProjectMemberStatus.JOINED
                ))
                .willReturn(memberships);

        List<ProjectSummaryResponse> responses = projectService.getMyProjects(1L, null);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getId)
                .containsExactly(10L, 20L);
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getName)
                .containsExactly("Wrap", "Graduation");
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getStatus)
                .containsExactly(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED);
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getColor)
                .containsOnly(Project.DEFAULT_COLOR);
        verify(projectMemberRepository)
                .findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
                        1L,
                        ProjectMemberStatus.JOINED
                );
        verifyNoInteractions(memberRepository, projectRepository);
    }

    @Test
    @DisplayName("내 프로젝트 목록 조회 성공 - 참여 프로젝트가 없으면 빈 목록을 반환한다")
    void getMyProjects_empty() {
        given(projectMemberRepository
                .findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
                        1L,
                        ProjectMemberStatus.JOINED
                ))
                .willReturn(List.of());

        List<ProjectSummaryResponse> responses = projectService.getMyProjects(1L, null);

        assertThat(responses).isEmpty();
        verify(projectMemberRepository)
                .findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
                        1L,
                        ProjectMemberStatus.JOINED
                );
        verifyNoInteractions(memberRepository, projectRepository);
    }

    @Test
    @DisplayName("프로젝트 상세 조회 성공 - 참여 중인 멤버에게 상세 정보를 반환한다")
    void getProject_success() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember membership = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(membership));

        ProjectResponse response = projectService.getProject(1L, 10L);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Wrap");
        assertThat(response.getDescription()).isEqualTo("프로젝트 설명");
        assertThat(response.getGoal()).isEqualTo("프로젝트 목표");
        assertThat(response.getSuccessCriteria()).isEqualTo("성공 기준");
        assertThat(response.getColor()).isEqualTo(Project.DEFAULT_COLOR);
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 상세 조회 실패 - 프로젝트가 없거나 삭제되었다")
    void getProject_projectNotFound() {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_NOT_FOUND));

        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verifyNoInteractions(memberRepository, projectMemberRepository);
    }

    @Test
    @DisplayName("프로젝트 상세 조회 실패 - 참여 중인 멤버가 아니다")
    void getProject_accessDenied() {
        Project project = project(10L, "Wrap");
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED));

        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 수정 성공 - OWNER가 프로젝트 정보를 변경한다")
    void update_success() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        ProjectUpdateRequest request = updateRequest(
                "Updated Wrap",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        ProjectResponse response = projectService.update(1L, 10L, request);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getName()).isEqualTo("Updated Wrap");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getGoal()).isEqualTo("Updated goal");
        assertThat(response.getSuccessCriteria()).isEqualTo("Updated criteria");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(response.getColor()).isEqualTo("#60C8F5");
        assertThat(project.getColor()).isEqualTo("#60C8F5");
        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 수정 실패 - OWNER가 아닌 멤버는 수정할 수 없다")
    void update_ownerRequired() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember joinedMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        ProjectUpdateRequest request = updateRequest(
                "Updated Wrap",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 31)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(joinedMember));

        assertThatThrownBy(() -> projectService.update(1L, 10L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));

        assertThat(project.getName()).isEqualTo("Wrap");
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 수정 실패 - 시작일이 종료일보다 늦다")
    void update_invalidDateRange() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        ProjectUpdateRequest request = updateRequest(
                "Updated Wrap",
                LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 9, 1)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectService.update(1L, 10L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.INVALID_DATE_RANGE));

        assertThat(project.getName()).isEqualTo("Wrap");
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 완료 성공 - OWNER가 진행 중인 프로젝트를 완료한다")
    void complete_success() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        ProjectResponse response = projectService.complete(1L, 10L);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(project.isCompleted()).isTrue();
        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 완료 실패 - OWNER가 아닌 멤버는 완료할 수 없다")
    void complete_ownerRequired() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember joinedMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(joinedMember));

        assertThatThrownBy(() -> projectService.complete(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));

        assertThat(project.isCompleted()).isFalse();
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 완료 실패 - 이미 완료된 프로젝트다")
    void complete_alreadyCompleted() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectService.complete(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ALREADY_COMPLETED));

        assertThat(project.getCompletedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 31, 18, 0));
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 재진행 성공 - OWNER가 완료 프로젝트를 다시 진행한다")
    void reopen_success() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        ProjectResponse response = projectService.reopen(1L, 10L);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(response.getCompletedAt()).isNull();
        assertThat(project.isCompleted()).isFalse();
        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 재진행 실패 - OWNER가 아닌 멤버는 재진행할 수 없다")
    void reopen_ownerRequired() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        ProjectMember joinedMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(joinedMember));

        assertThatThrownBy(() -> projectService.reopen(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));

        assertThat(project.isCompleted()).isTrue();
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 재진행 실패 - 완료되지 않은 프로젝트다")
    void reopen_notCompleted() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectService.reopen(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_NOT_COMPLETED));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getCompletedAt()).isNull();
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 삭제 성공 - OWNER가 프로젝트를 Soft Delete한다")
    void delete_success() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember owner = ProjectMember.createOwner(
                member,
                project,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));

        projectService.delete(1L, 10L);

        assertThat(project.isDeleted()).isTrue();
        assertThat(project.getDeletedAt()).isNotNull();
        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
        verify(projectRepository, never()).delete(any(Project.class));
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 삭제 실패 - OWNER가 아닌 멤버는 삭제할 수 없다")
    void delete_ownerRequired() {
        Member member = mock(Member.class);
        Project project = project(10L, "Wrap");
        ProjectMember joinedMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(joinedMember));

        assertThatThrownBy(() -> projectService.delete(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));

        assertThat(project.isDeleted()).isFalse();
        verify(projectRepository, never()).delete(any(Project.class));
        verifyNoInteractions(memberRepository);
    }

    @Test
    @DisplayName("프로젝트 삭제 실패 - 프로젝트가 없거나 이미 삭제되었다")
    void delete_projectNotFound() {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_NOT_FOUND));

        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verifyNoInteractions(memberRepository, projectMemberRepository);
    }

    @Test
    void update_colorOnlyPreservesOtherFields() {
        Project project = project(10L, "Wrap");
        givenUpdateOwner(project);
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(request, "color", "#A78BFA");

        ProjectResponse response = projectService.update(1L, 10L, request);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getColor()).isEqualTo("#A78BFA");
        assertThat(response.getName()).isEqualTo("Wrap");
        assertThat(response.getDescription()).isEqualTo("프로젝트 설명");
        assertThat(response.getGoal()).isEqualTo("프로젝트 목표");
        assertThat(response.getSuccessCriteria()).isEqualTo("성공 기준");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @ParameterizedTest
    @CsvSource({
            "startDate, 2026-08-31, 2026-08-31, 2026-08-31",
            "endDate, 2026-07-01, 2026-07-01, 2026-07-01"
    })
    void update_singleDateAcceptsValidMergedRange(
            String field, LocalDate value, LocalDate expectedStart, LocalDate expectedEnd
    ) {
        Project project = project(10L, "Wrap");
        givenUpdateOwner(project);
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(request, field, value);

        ProjectResponse response = projectService.update(1L, 10L, request);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getStartDate()).isEqualTo(expectedStart);
        assertThat(response.getEndDate()).isEqualTo(expectedEnd);
        assertThat(response.getName()).isEqualTo("Wrap");
    }

    @ParameterizedTest
    @CsvSource({"startDate, 2026-09-01", "endDate, 2026-06-30"})
    void update_singleDateRejectsInvalidMergedRange(String field, LocalDate value) {
        Project project = project(10L, "Wrap");
        givenUpdateOwner(project);
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(request, field, value);
        ReflectionTestUtils.setField(request, "name", "변경되면 안 되는 이름");

        assertThatThrownBy(() -> projectService.update(1L, 10L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.INVALID_DATE_RANGE));

        assertThat(project.getName()).isEqualTo("Wrap");
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("완료된 프로젝트는 OWNER도 수정할 수 없고 기존 정보를 유지한다")
    void update_completedProjectRejected() {
        Project project = project(10L, "Wrap");
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 31, 18, 0);
        project.complete(completedAt);
        givenUpdateOwner(project);
        ProjectUpdateRequest request = updateRequest(
                "Updated Wrap", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));

        assertThatThrownBy(() -> projectService.update(1L, 10L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ALREADY_COMPLETED));

        assertThat(project.getName()).isEqualTo("Wrap");
        assertThat(project.getDescription()).isEqualTo("프로젝트 설명");
        assertThat(project.getGoal()).isEqualTo("프로젝트 목표");
        assertThat(project.getSuccessCriteria()).isEqualTo("성공 기준");
        assertThat(project.getColor()).isEqualTo(Project.DEFAULT_COLOR);
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        assertThat(project.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("완료 프로젝트를 재개한 뒤에는 OWNER가 수정할 수 있다")
    void update_reopenedProjectAllowed() {
        Project project = project(10L, "Wrap");
        project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        givenUpdateOwner(project);
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(request, "name", "Reopened Wrap");

        projectService.reopen(1L, 10L);
        ProjectResponse response = projectService.update(1L, 10L, request);

        assertThat(response.getMyRole()).isEqualTo(ProjectMemberRole.OWNER);

        assertThat(response.getName()).isEqualTo("Reopened Wrap");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(project.getCompletedAt()).isNull();
        assertThat(project.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(project.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    @DisplayName("완료 프로젝트도 수정 권한을 먼저 확인한다")
    void update_completedProjectRequiresOwnerFirst() {
        Project project = project(10L, "Wrap");
        project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        ProjectMember joinedMember = ProjectMember.join(
                mock(Member.class), project, ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 7, 1, 9, 0));
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L, 10L, ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(joinedMember));

        assertThatThrownBy(() -> projectService.update(1L, 10L, new ProjectUpdateRequest()))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));
    }

    @ParameterizedTest
    @EnumSource(ProjectStatus.class)
    @DisplayName("상태 필터로 조회한 프로젝트를 요약 응답으로 반환한다")
    void getMyProjects_withStatus(ProjectStatus status) {
        Project project = project(10L, "Wrap");
        if (status == ProjectStatus.COMPLETED) {
            project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        }
        ProjectMember membership = ProjectMember.createOwner(
                mock(Member.class), project, LocalDateTime.of(2026, 7, 1, 9, 0));
        given(projectMemberRepository.findAllByMemberIdAndStatusAndProjectStatusAndProjectDeletedAtIsNull(
                1L, ProjectMemberStatus.JOINED, status
        )).willReturn(List.of(membership));

        List<ProjectSummaryResponse> responses = projectService.getMyProjects(1L, status);

        assertThat(responses).extracting(ProjectSummaryResponse::getId).containsExactly(10L);
        assertThat(responses).extracting(ProjectSummaryResponse::getStatus).containsExactly(status);
        verify(projectMemberRepository, never())
                .findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(any(), any());
    }

    @ParameterizedTest
    @EnumSource(ProjectStatus.class)
    @DisplayName("해당 상태의 프로젝트가 없으면 빈 목록을 반환한다")
    void getMyProjects_withStatusEmpty(ProjectStatus status) {
        given(projectMemberRepository.findAllByMemberIdAndStatusAndProjectStatusAndProjectDeletedAtIsNull(
                1L, ProjectMemberStatus.JOINED, status
        )).willReturn(List.of());

        assertThat(projectService.getMyProjects(1L, status)).isEmpty();
    }

    @Test
    @DisplayName("같은 프로젝트를 조회해도 요청자마다 자신의 현재 권한을 반환한다")
    void getProject_returnsRequestingMembersCurrentRole() {
        Project project = project(10L, "Wrap");
        LocalDateTime joinedAt = LocalDateTime.of(2026, 7, 1, 9, 0);
        ProjectMember owner = ProjectMember.createOwner(mock(Member.class), project, joinedAt);
        ProjectMember member = ProjectMember.join(
                mock(Member.class), project, ProjectMemberRole.MEMBER, joinedAt);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L, 10L, ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                2L, 10L, ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(member));

        assertThat(projectService.getProject(1L, 10L).getMyRole()).isEqualTo(ProjectMemberRole.OWNER);
        assertThat(projectService.getProject(2L, 10L).getMyRole()).isEqualTo(ProjectMemberRole.MEMBER);

        member.changeRole(ProjectMemberRole.OWNER);

        assertThat(projectService.getProject(2L, 10L).getMyRole()).isEqualTo(ProjectMemberRole.OWNER);
    }

    private void givenUpdateOwner(Project project) {
        ProjectMember owner = ProjectMember.createOwner(
                mock(Member.class), project, LocalDateTime.of(2026, 7, 1, 9, 0));
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L, 10L, ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));
    }

    private ProjectCreateRequest createRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ProjectCreateRequest request = new ProjectCreateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "description", "프로젝트 설명");
        ReflectionTestUtils.setField(request, "goal", "프로젝트 목표");
        ReflectionTestUtils.setField(request, "successCriteria", "성공 기준");
        ReflectionTestUtils.setField(request, "startDate", startDate);
        ReflectionTestUtils.setField(request, "endDate", endDate);
        ReflectionTestUtils.setField(request, "color", "#A78BFA");
        return request;
    }

    private ProjectUpdateRequest updateRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "description", "Updated description");
        ReflectionTestUtils.setField(request, "goal", "Updated goal");
        ReflectionTestUtils.setField(request, "successCriteria", "Updated criteria");
        ReflectionTestUtils.setField(request, "startDate", startDate);
        ReflectionTestUtils.setField(request, "endDate", endDate);
        ReflectionTestUtils.setField(request, "color", "#60C8F5");
        return request;
    }

    private Project project(Long id, String name) {
        Project project = Project.create(
                name,
                "프로젝트 설명",
                "프로젝트 목표",
                "성공 기준",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31),
                Project.DEFAULT_COLOR
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }
}
