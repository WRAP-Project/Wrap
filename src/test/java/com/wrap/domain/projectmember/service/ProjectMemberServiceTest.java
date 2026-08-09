package com.wrap.domain.projectmember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.dto.request.ProjectMemberRoleUpdateRequest;
import com.wrap.domain.projectmember.dto.response.ProjectMemberResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Test
    @DisplayName("프로젝트 멤버 목록 조회 성공 - 완료 프로젝트의 JOINED 멤버도 조회한다")
    void getProjectMembers_success() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 8, 1, 18, 0));
        Member ownerMember = member(1L, "owner", "owner.png");
        Member joinedMember = member(2L, "member", null);
        ProjectMember owner = projectMember(
                100L,
                ownerMember,
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMember member = projectMember(
                200L,
                joinedMember,
                project,
                ProjectMemberRole.MEMBER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));
        given(projectMemberRepository.findAllByProjectIdAndStatus(
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(List.of(owner, member));

        List<ProjectMemberResponse> responses =
                projectMemberService.getProjectMembers(1L, 10L);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ProjectMemberResponse::getProjectMemberId)
                .containsExactly(100L, 200L);
        assertThat(responses)
                .extracting(ProjectMemberResponse::getNickname)
                .containsExactly("owner", "member");
        assertThat(responses)
                .extracting(ProjectMemberResponse::getRole)
                .containsExactly(ProjectMemberRole.OWNER, ProjectMemberRole.MEMBER);
        assertThat(responses)
                .extracting(ProjectMemberResponse::getStatus)
                .containsOnly(ProjectMemberStatus.JOINED);
        verify(projectMemberRepository).findAllByProjectIdAndStatus(
                10L,
                ProjectMemberStatus.JOINED
        );
    }

    @Test
    @DisplayName("프로젝트 멤버 목록 조회 실패 - 프로젝트가 없거나 삭제되었다")
    void getProjectMembers_projectNotFound() {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.getProjectMembers(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_NOT_FOUND));

        verify(projectRepository).findByIdAndDeletedAtIsNull(10L);
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    @DisplayName("프로젝트 멤버 목록 조회 실패 - 요청자가 JOINED 멤버가 아니다")
    void getProjectMembers_accessDenied() {
        Project project = project(10L);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.getProjectMembers(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED));

        verify(projectMemberRepository).findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        );
    }

    @Test
    @DisplayName("프로젝트 멤버 역할 변경 성공 - OWNER가 MEMBER를 OWNER로 변경한다")
    void changeRole_memberToOwner_success() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMember target = projectMember(
                200L,
                member(2L, "target", null),
                project,
                ProjectMemberRole.MEMBER
        );
        ProjectMemberRoleUpdateRequest request = roleRequest(ProjectMemberRole.OWNER);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(200L, 10L))
                .willReturn(Optional.of(target));

        ProjectMemberResponse response =
                projectMemberService.changeRole(1L, 10L, 200L, request);

        assertThat(response.getRole()).isEqualTo(ProjectMemberRole.OWNER);
        assertThat(target.getRole()).isEqualTo(ProjectMemberRole.OWNER);
    }

    @Test
    @DisplayName("프로젝트 멤버 역할 변경 성공 - OWNER가 두 명이면 자신의 역할을 변경한다")
    void changeRole_selfDemotion_success() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMemberRoleUpdateRequest request = roleRequest(ProjectMemberRole.MEMBER);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(100L, 10L))
                .willReturn(Optional.of(requester));
        given(projectMemberRepository.countByProjectIdAndRoleAndStatus(
                10L,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        )).willReturn(2L);

        ProjectMemberResponse response =
                projectMemberService.changeRole(1L, 10L, 100L, request);

        assertThat(response.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(requester.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
    }

    @Test
    @DisplayName("프로젝트 멤버 역할 변경 실패 - 마지막 OWNER는 MEMBER가 될 수 없다")
    void changeRole_lastOwner() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMemberRoleUpdateRequest request = roleRequest(ProjectMemberRole.MEMBER);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(100L, 10L))
                .willReturn(Optional.of(requester));
        given(projectMemberRepository.countByProjectIdAndRoleAndStatus(
                10L,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        )).willReturn(1L);

        assertThatThrownBy(() ->
                projectMemberService.changeRole(1L, 10L, 100L, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.LAST_PROJECT_OWNER));

        assertThat(requester.getRole()).isEqualTo(ProjectMemberRole.OWNER);
    }

    @Test
    @DisplayName("프로젝트 멤버 역할 변경 실패 - 요청자가 OWNER가 아니다")
    void changeRole_ownerRequired() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.MEMBER
        );
        ProjectMemberRoleUpdateRequest request = roleRequest(ProjectMemberRole.OWNER);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));

        assertThatThrownBy(() ->
                projectMemberService.changeRole(1L, 10L, 200L, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));
    }

    @Test
    @DisplayName("프로젝트 멤버 역할 변경 실패 - 완료된 프로젝트다")
    void changeRole_projectCompleted() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 8, 1, 18, 0));
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMemberRoleUpdateRequest request = roleRequest(ProjectMemberRole.OWNER);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));

        assertThatThrownBy(() ->
                projectMemberService.changeRole(1L, 10L, 200L, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ALREADY_COMPLETED));
    }

    @Test
    @DisplayName("프로젝트 멤버 역할 변경 실패 - 대상이 JOINED 멤버가 아니다")
    void changeRole_targetNotFound() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMember target = projectMember(
                200L,
                member(2L, "target", null),
                project,
                ProjectMemberRole.MEMBER
        );
        target.leave();
        ProjectMemberRoleUpdateRequest request = roleRequest(ProjectMemberRole.OWNER);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(200L, 10L))
                .willReturn(Optional.of(target));

        assertThatThrownBy(() ->
                projectMemberService.changeRole(1L, 10L, 200L, request)
        )
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("프로젝트 탈퇴 성공 - MEMBER가 프로젝트를 나간다")
    void leaveProject_member_success() {
        Project project = project(10L);
        ProjectMember projectMember = projectMember(
                100L,
                member(1L, "member", null),
                project,
                ProjectMemberRole.MEMBER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(projectMember));

        projectMemberService.leaveProject(1L, 10L);

        assertThat(projectMember.getStatus()).isEqualTo(ProjectMemberStatus.LEFT);
    }

    @Test
    @DisplayName("프로젝트 탈퇴 성공 - OWNER가 두 명 이상이면 OWNER도 나갈 수 있다")
    void leaveProject_owner_success() {
        Project project = project(10L);
        ProjectMember projectMember = projectMember(
                100L,
                member(1L, "owner", null),
                project,
                ProjectMemberRole.OWNER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(projectMember));
        given(projectMemberRepository.countByProjectIdAndRoleAndStatus(
                10L,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        )).willReturn(2L);

        projectMemberService.leaveProject(1L, 10L);

        assertThat(projectMember.getStatus()).isEqualTo(ProjectMemberStatus.LEFT);
    }

    @Test
    @DisplayName("프로젝트 탈퇴 실패 - 마지막 OWNER는 나갈 수 없다")
    void leaveProject_lastOwner() {
        Project project = project(10L);
        ProjectMember projectMember = projectMember(
                100L,
                member(1L, "owner", null),
                project,
                ProjectMemberRole.OWNER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(projectMember));
        given(projectMemberRepository.countByProjectIdAndRoleAndStatus(
                10L,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        )).willReturn(1L);

        assertThatThrownBy(() -> projectMemberService.leaveProject(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.LAST_PROJECT_OWNER));

        assertThat(projectMember.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
    }

    @Test
    @DisplayName("프로젝트 탈퇴 실패 - 완료된 프로젝트에서는 나갈 수 없다")
    void leaveProject_projectCompleted() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 8, 1, 18, 0));
        ProjectMember projectMember = projectMember(
                100L,
                member(1L, "member", null),
                project,
                ProjectMemberRole.MEMBER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(projectMember));

        assertThatThrownBy(() -> projectMemberService.leaveProject(1L, 10L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ALREADY_COMPLETED));

        assertThat(projectMember.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
    }

    @Test
    @DisplayName("프로젝트 멤버 내보내기 성공 - OWNER가 MEMBER를 내보낸다")
    void removeMember_success() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "owner", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMember target = projectMember(
                200L,
                member(2L, "member", null),
                project,
                ProjectMemberRole.MEMBER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(200L, 10L))
                .willReturn(Optional.of(target));

        projectMemberService.removeMember(1L, 10L, 200L);

        assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.LEFT);
    }

    @Test
    @DisplayName("프로젝트 멤버 내보내기 실패 - 요청자가 OWNER가 아니다")
    void removeMember_ownerRequired() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "member", null),
                project,
                ProjectMemberRole.MEMBER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 10L, 200L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED));
    }

    @Test
    @DisplayName("프로젝트 멤버 내보내기 실패 - 완료된 프로젝트다")
    void removeMember_projectCompleted() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 8, 1, 18, 0));
        ProjectMember requester = projectMember(
                100L,
                member(1L, "owner", null),
                project,
                ProjectMemberRole.OWNER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));

        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 10L, 200L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_ALREADY_COMPLETED));
    }

    @Test
    @DisplayName("프로젝트 멤버 내보내기 실패 - OWNER는 내보낼 수 없다")
    void removeMember_ownerCannotBeRemoved() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "requester", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMember target = projectMember(
                200L,
                member(2L, "target", null),
                project,
                ProjectMemberRole.OWNER
        );
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(200L, 10L))
                .willReturn(Optional.of(target));

        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 10L, 200L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_OWNER_CANNOT_BE_REMOVED));

        assertThat(target.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
    }

    @Test
    @DisplayName("프로젝트 멤버 내보내기 실패 - 대상이 JOINED 멤버가 아니다")
    void removeMember_targetNotFound() {
        Project project = project(10L);
        ProjectMember requester = projectMember(
                100L,
                member(1L, "owner", null),
                project,
                ProjectMemberRole.OWNER
        );
        ProjectMember target = projectMember(
                200L,
                member(2L, "target", null),
                project,
                ProjectMemberRole.MEMBER
        );
        target.leave();
        given(projectRepository.findByIdAndDeletedAtIsNull(10L))
                .willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(requester));
        given(projectMemberRepository.findByIdAndProjectId(200L, 10L))
                .willReturn(Optional.of(target));

        assertThatThrownBy(() -> projectMemberService.removeMember(1L, 10L, 200L))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }

    private Member member(Long id, String nickname, String profileImage) {
        Member member = Member.builder()
                .email(nickname + "@example.com")
                .password("encoded-password")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "profileImage", profileImage);
        return member;
    }

    private Project project(Long id) {
        Project project = Project.create(
                "Wrap",
                "Project description",
                "Project goal",
                "Success criteria",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember projectMember(
            Long id,
            Member member,
            Project project,
            ProjectMemberRole role
    ) {
        ProjectMember projectMember = ProjectMember.join(
                member,
                project,
                role,
                LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        ReflectionTestUtils.setField(projectMember, "id", id);
        return projectMember;
    }

    private ProjectMemberRoleUpdateRequest roleRequest(ProjectMemberRole role) {
        ProjectMemberRoleUpdateRequest request = new ProjectMemberRoleUpdateRequest();
        ReflectionTestUtils.setField(request, "role", role);
        return request;
    }
}
