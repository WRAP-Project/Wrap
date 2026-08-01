package com.wrap.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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

        assertThat(response.getName()).isEqualTo("Wrap");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        ArgumentCaptor<ProjectMember> ownerCaptor =
                ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(ownerCaptor.capture());

        ProjectMember owner = ownerCaptor.getValue();
        assertThat(owner.getMember()).isSameAs(member);
        assertThat(owner.getProject().getName()).isEqualTo("Wrap");
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

        List<ProjectSummaryResponse> responses = projectService.getMyProjects(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getId)
                .containsExactly(10L, 20L);
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getName)
                .containsExactly("Wrap", "Graduation");
        assertThat(responses)
                .extracting(ProjectSummaryResponse::getStatus)
                .containsOnly(ProjectStatus.IN_PROGRESS);
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

        List<ProjectSummaryResponse> responses = projectService.getMyProjects(1L);

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

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Wrap");
        assertThat(response.getDescription()).isEqualTo("프로젝트 설명");
        assertThat(response.getGoal()).isEqualTo("프로젝트 목표");
        assertThat(response.getSuccessCriteria()).isEqualTo("성공 기준");
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

        assertThat(response.getName()).isEqualTo("Updated Wrap");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getGoal()).isEqualTo("Updated goal");
        assertThat(response.getSuccessCriteria()).isEqualTo("Updated criteria");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 31));
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
        return request;
    }

    private Project project(Long id, String name) {
        Project project = Project.create(
                name,
                "프로젝트 설명",
                "프로젝트 목표",
                "성공 기준",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }
}
