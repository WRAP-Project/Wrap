package com.wrap.domain.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.wrap.domain.invitation.dto.request.InvitationCreateRequest;
import com.wrap.domain.invitation.dto.response.InvitationResponse;
import com.wrap.domain.invitation.dto.response.ReceivedInvitationResponse;
import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.invitation.repository.InvitationRepository;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void getReceivedInvitations_success() {
        Project newProject = project(20L, "New Project");
        Project oldProject = project(10L, "Old Project");
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        Invitation newInvitation = invitation(
                200L,
                newProject,
                inviter,
                invitee,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
        Invitation oldInvitation = invitation(
                100L,
                oldProject,
                inviter,
                invitee,
                LocalDateTime.of(2026, 8, 17, 10, 0)
        );
        given(invitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(2L))
                .willReturn(List.of(newInvitation, oldInvitation));

        List<ReceivedInvitationResponse> responses =
                invitationService.getReceivedInvitations(2L);

        assertThat(responses)
                .extracting(ReceivedInvitationResponse::getInvitationId)
                .containsExactly(200L, 100L);
        assertThat(responses.get(0).getProjectName()).isEqualTo("New Project");
        assertThat(responses.get(0).getInviterNickname()).isEqualTo("owner");
        assertThat(responses.get(0).getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(responses.get(0).getStatus()).isEqualTo(InvitationStatus.INVITED);
        verify(invitationRepository).findAllByInviteeIdOrderByCreatedAtDesc(2L);
    }

    @Test
    void getReceivedInvitations_empty() {
        given(invitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(2L))
                .willReturn(List.of());

        List<ReceivedInvitationResponse> responses =
                invitationService.getReceivedInvitations(2L);

        assertThat(responses).isEmpty();
        verify(invitationRepository).findAllByInviteeIdOrderByCreatedAtDesc(2L);
    }

    @Test
    void create_success() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        ProjectMember owner = projectMember(inviter, project, ProjectMemberRole.OWNER);
        InvitationCreateRequest request = request("  invitee@example.com  ", ProjectMemberRole.MEMBER);

        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(owner));
        given(memberRepository.findByEmail("invitee@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                2L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(false);
        given(invitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                10L,
                2L,
                InvitationStatus.INVITED
        )).willReturn(false);
        given(invitationRepository.save(any(Invitation.class))).willAnswer(invocation -> {
            Invitation invitation = invocation.getArgument(0);
            ReflectionTestUtils.setField(invitation, "id", 100L);
            ReflectionTestUtils.setField(
                    invitation,
                    "createdAt",
                    LocalDateTime.of(2026, 8, 16, 10, 0)
            );
            return invitation;
        });

        InvitationResponse response = invitationService.create(1L, 10L, request);

        assertThat(response.getInvitationId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getInviteeMemberId()).isEqualTo(2L);
        assertThat(response.getInviteeEmail()).isEqualTo("invitee@example.com");
        assertThat(response.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.INVITED);
        verify(memberRepository).findByEmail("invitee@example.com");
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void create_projectNotFound() {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.empty());

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.PROJECT_NOT_FOUND
        );

        verifyNoInteractions(memberRepository, projectMemberRepository, invitationRepository);
    }

    @Test
    void create_accessDenied() {
        Project project = project(10L);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.empty());

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.PROJECT_ACCESS_DENIED
        );

        verifyNoInteractions(memberRepository, invitationRepository);
    }

    @Test
    void create_ownerRequired() {
        Project project = project(10L);
        Member requester = member(1L, "member@example.com", "member");
        ProjectMember projectMember = projectMember(
                requester,
                project,
                ProjectMemberRole.MEMBER
        );
        givenProjectAndRequester(project, projectMember);

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.PROJECT_OWNER_REQUIRED
        );

        verifyNoInteractions(memberRepository, invitationRepository);
    }

    @Test
    void create_projectCompleted() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 8, 16, 9, 0));
        Member inviter = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(inviter, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );

        verifyNoInteractions(memberRepository, invitationRepository);
    }

    @Test
    void create_inviteeNotFound() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(inviter, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);
        given(memberRepository.findByEmail("invitee@example.com")).willReturn(Optional.empty());

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.MEMBER_NOT_FOUND
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void create_deletedInviteeIsNotFound() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        ReflectionTestUtils.setField(invitee, "deletedAt", LocalDateTime.now());
        ProjectMember owner = projectMember(inviter, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);
        given(memberRepository.findByEmail("invitee@example.com")).willReturn(Optional.of(invitee));

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.MEMBER_NOT_FOUND
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void create_projectMemberAlreadyExists() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        ProjectMember owner = projectMember(inviter, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);
        given(memberRepository.findByEmail("invitee@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                2L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(true);

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS
        );

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void create_invitationAlreadyExists() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        ProjectMember owner = projectMember(inviter, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);
        given(memberRepository.findByEmail("invitee@example.com")).willReturn(Optional.of(invitee));
        given(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                2L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(false);
        given(invitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                10L,
                2L,
                InvitationStatus.INVITED
        )).willReturn(true);

        assertError(
                () -> invitationService.create(1L, 10L, request()),
                ErrorCode.INVITATION_ALREADY_EXISTS
        );

        verify(invitationRepository, never()).save(any());
    }

    private void givenProjectAndRequester(Project project, ProjectMember projectMember) {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.of(projectMember));
    }

    private InvitationCreateRequest request() {
        return request("invitee@example.com", ProjectMemberRole.MEMBER);
    }

    private InvitationCreateRequest request(String email, ProjectMemberRole role) {
        InvitationCreateRequest request = new InvitationCreateRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "role", role);
        return request;
    }

    private Project project(Long id) {
        return project(id, "Wrap");
    }

    private Project project(Long id, String name) {
        Project project = Project.create(
                name,
                null,
                null,
                null,
                null,
                null,
                Project.DEFAULT_COLOR
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private Invitation invitation(
            Long id,
            Project project,
            Member inviter,
            Member invitee,
            LocalDateTime createdAt
    ) {
        Invitation invitation = Invitation.create(
                project,
                inviter,
                invitee,
                ProjectMemberRole.MEMBER
        );
        ReflectionTestUtils.setField(invitation, "id", id);
        ReflectionTestUtils.setField(invitation, "createdAt", createdAt);
        return invitation;
    }

    private Member member(Long id, String email, String nickname) {
        Member member = Member.builder()
                .email(email)
                .password("password1234")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private ProjectMember projectMember(
            Member member,
            Project project,
            ProjectMemberRole role
    ) {
        return ProjectMember.join(member, project, role, LocalDateTime.now());
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(errorCode));
    }
}
