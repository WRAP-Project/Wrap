package com.wrap.domain.projectmember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
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
}
