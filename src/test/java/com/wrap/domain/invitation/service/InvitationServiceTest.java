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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
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
    void getSentInvitations_success() {
        Project project = project(10L);
        Member ownerMember = member(1L, "owner@example.com", "owner");
        Member newInvitee = member(2L, "new@example.com", "new");
        Member oldInvitee = member(3L, "old@example.com", "old");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        Invitation newInvitation = invitation(
                200L,
                project,
                ownerMember,
                newInvitee,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
        Invitation oldInvitation = invitation(
                100L,
                project,
                ownerMember,
                oldInvitee,
                LocalDateTime.of(2026, 8, 17, 10, 0)
        );
        givenProjectAndRequester(project, owner);
        given(invitationRepository.findAllByProjectIdOrderByCreatedAtDesc(10L))
                .willReturn(List.of(newInvitation, oldInvitation));

        List<InvitationResponse> responses =
                invitationService.getSentInvitations(1L, 10L);

        assertThat(responses)
                .extracting(InvitationResponse::getInvitationId)
                .containsExactly(200L, 100L);
        assertThat(responses.get(0).getProjectId()).isEqualTo(10L);
        assertThat(responses.get(0).getInviteeMemberId()).isEqualTo(2L);
        assertThat(responses.get(0).getInviteeEmail()).isEqualTo("new@example.com");
        assertThat(responses.get(0).getStatus()).isEqualTo(InvitationStatus.INVITED);
        verify(invitationRepository).findAllByProjectIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void getSentInvitations_empty() {
        Project project = project(10L);
        Member ownerMember = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);
        given(invitationRepository.findAllByProjectIdOrderByCreatedAtDesc(10L))
                .willReturn(List.of());

        List<InvitationResponse> responses =
                invitationService.getSentInvitations(1L, 10L);

        assertThat(responses).isEmpty();
        verify(invitationRepository).findAllByProjectIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void getSentInvitations_projectNotFound() {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.empty());

        assertError(
                () -> invitationService.getSentInvitations(1L, 10L),
                ErrorCode.PROJECT_NOT_FOUND
        );

        verifyNoInteractions(projectMemberRepository, invitationRepository);
    }

    @Test
    void getSentInvitations_accessDenied() {
        Project project = project(10L);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.empty());

        assertError(
                () -> invitationService.getSentInvitations(1L, 10L),
                ErrorCode.PROJECT_ACCESS_DENIED
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void getSentInvitations_ownerRequired() {
        Project project = project(10L);
        Member member = member(1L, "member@example.com", "member");
        ProjectMember projectMember = projectMember(
                member,
                project,
                ProjectMemberRole.MEMBER
        );
        givenProjectAndRequester(project, projectMember);

        assertError(
                () -> invitationService.getSentInvitations(1L, 10L),
                ErrorCode.PROJECT_OWNER_REQUIRED
        );

        verifyNoInteractions(invitationRepository);
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

    @Test
    void accept_success_newProjectMember() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        Invitation invitation = invitation(
                100L,
                project,
                inviter,
                invitee,
                LocalDateTime.of(2026, 8, 22, 10, 0)
        );
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));
        given(projectMemberRepository.findByMemberIdAndProjectId(2L, 10L))
                .willReturn(Optional.empty());

        InvitationResponse response = invitationService.accept(2L, 100L);

        ArgumentCaptor<ProjectMember> projectMemberCaptor =
                ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(projectMemberCaptor.capture());
        ProjectMember savedProjectMember = projectMemberCaptor.getValue();
        assertThat(savedProjectMember.getMember()).isSameAs(invitee);
        assertThat(savedProjectMember.getProject()).isSameAs(project);
        assertThat(savedProjectMember.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(savedProjectMember.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
        assertThat(savedProjectMember.getJoinedAt()).isNotNull();
        assertThat(response.getInvitationId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void accept_success_rejoinLeftProjectMember() {
        Project project = project(10L);
        Member inviter = member(1L, "owner@example.com", "owner");
        Member invitee = member(2L, "invitee@example.com", "invitee");
        Invitation invitation = invitation(
                100L,
                project,
                inviter,
                invitee,
                LocalDateTime.of(2026, 8, 22, 10, 0)
        );
        LocalDateTime previousJoinedAt = LocalDateTime.of(2026, 8, 1, 10, 0);
        ProjectMember leftProjectMember = ProjectMember.join(
                invitee,
                project,
                ProjectMemberRole.OWNER,
                previousJoinedAt
        );
        leftProjectMember.leave();
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));
        given(projectMemberRepository.findByMemberIdAndProjectId(2L, 10L))
                .willReturn(Optional.of(leftProjectMember));

        InvitationResponse response = invitationService.accept(2L, 100L);

        assertThat(leftProjectMember.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
        assertThat(leftProjectMember.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(leftProjectMember.getJoinedAt()).isAfter(previousJoinedAt);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void accept_invitationNotFound() {
        given(invitationRepository.findByIdForUpdate(100L)).willReturn(Optional.empty());

        assertError(
                () -> invitationService.accept(2L, 100L),
                ErrorCode.INVITATION_NOT_FOUND
        );

        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    void accept_accessDenied() {
        Invitation invitation = invitationForAccept();
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.accept(3L, 100L),
                ErrorCode.INVITATION_ACCESS_DENIED
        );

        verifyNoInteractions(projectMemberRepository);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
    }

    @ParameterizedTest
    @EnumSource(
            value = InvitationStatus.class,
            names = {"ACCEPTED", "REJECTED", "CANCELED"}
    )
    void accept_alreadyProcessed(InvitationStatus status) {
        Invitation invitation = invitationForAccept();
        ReflectionTestUtils.setField(invitation, "status", status);
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.accept(2L, 100L),
                ErrorCode.INVITATION_ALREADY_PROCESSED
        );

        verifyNoInteractions(projectMemberRepository);
        assertThat(invitation.getStatus()).isEqualTo(status);
    }

    @Test
    void accept_deletedProject() {
        Invitation invitation = invitationForAccept();
        invitation.getProject().softDelete(LocalDateTime.of(2026, 8, 22, 11, 0));
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.accept(2L, 100L),
                ErrorCode.PROJECT_NOT_FOUND
        );

        verifyNoInteractions(projectMemberRepository);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
    }

    @Test
    void accept_completedProject() {
        Invitation invitation = invitationForAccept();
        invitation.getProject().complete(LocalDateTime.of(2026, 8, 22, 11, 0));
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.accept(2L, 100L),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );

        verifyNoInteractions(projectMemberRepository);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
    }

    @Test
    void accept_projectMemberAlreadyExists() {
        Invitation invitation = invitationForAccept();
        ProjectMember joinedProjectMember = ProjectMember.join(
                invitation.getInvitee(),
                invitation.getProject(),
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));
        given(projectMemberRepository.findByMemberIdAndProjectId(2L, 10L))
                .willReturn(Optional.of(joinedProjectMember));

        assertError(
                () -> invitationService.accept(2L, 100L),
                ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS
        );

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
    }

    @Test
    void reject_success() {
        Invitation invitation = invitationForAccept();
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        InvitationResponse response = invitationService.reject(2L, 100L);

        assertThat(response.getInvitationId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    void reject_invitationNotFound() {
        given(invitationRepository.findByIdForUpdate(100L)).willReturn(Optional.empty());

        assertError(
                () -> invitationService.reject(2L, 100L),
                ErrorCode.INVITATION_NOT_FOUND
        );

        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    void reject_accessDenied() {
        Invitation invitation = invitationForAccept();
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.reject(3L, 100L),
                ErrorCode.INVITATION_ACCESS_DENIED
        );

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
        verifyNoInteractions(projectMemberRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = InvitationStatus.class,
            names = {"ACCEPTED", "REJECTED", "CANCELED"}
    )
    void reject_alreadyProcessed(InvitationStatus status) {
        Invitation invitation = invitationForAccept();
        ReflectionTestUtils.setField(invitation, "status", status);
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.reject(2L, 100L),
                ErrorCode.INVITATION_ALREADY_PROCESSED
        );

        assertThat(invitation.getStatus()).isEqualTo(status);
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    void reject_deletedProject() {
        Invitation invitation = invitationForAccept();
        invitation.getProject().softDelete(LocalDateTime.of(2026, 8, 22, 11, 0));
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.reject(2L, 100L),
                ErrorCode.PROJECT_NOT_FOUND
        );

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    void reject_completedProject() {
        Invitation invitation = invitationForAccept();
        invitation.getProject().complete(LocalDateTime.of(2026, 8, 22, 11, 0));
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.reject(2L, 100L),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
        verifyNoInteractions(projectMemberRepository);
    }

    @Test
    void cancel_success() {
        Project project = project(10L);
        Member ownerMember = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        Invitation invitation = invitation(
                100L,
                project,
                ownerMember,
                member(2L, "invitee@example.com", "invitee"),
                LocalDateTime.of(2026, 8, 22, 10, 0)
        );
        givenProjectAndRequester(project, owner);
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        invitationService.cancel(1L, 10L, 100L);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELED);
    }

    @Test
    void cancel_projectNotFound() {
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.empty());

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.PROJECT_NOT_FOUND
        );

        verifyNoInteractions(projectMemberRepository, invitationRepository);
    }

    @Test
    void cancel_accessDenied() {
        Project project = project(10L);
        given(projectRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(project));
        given(projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).willReturn(Optional.empty());

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.PROJECT_ACCESS_DENIED
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void cancel_ownerRequired() {
        Project project = project(10L);
        Member member = member(1L, "member@example.com", "member");
        ProjectMember projectMember = projectMember(
                member,
                project,
                ProjectMemberRole.MEMBER
        );
        givenProjectAndRequester(project, projectMember);

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.PROJECT_OWNER_REQUIRED
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void cancel_completedProject() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 8, 22, 11, 0));
        Member ownerMember = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void cancel_invitationNotFound() {
        Project project = project(10L);
        Member ownerMember = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        givenProjectAndRequester(project, owner);
        given(invitationRepository.findByIdForUpdate(100L)).willReturn(Optional.empty());

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.INVITATION_NOT_FOUND
        );
    }

    @Test
    void cancel_invitationBelongsToDifferentProject() {
        Project project = project(10L);
        Member ownerMember = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        Invitation invitation = invitation(
                100L,
                project(20L, "Other Project"),
                ownerMember,
                member(2L, "invitee@example.com", "invitee"),
                LocalDateTime.of(2026, 8, 22, 10, 0)
        );
        givenProjectAndRequester(project, owner);
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.INVITATION_NOT_FOUND
        );

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
    }

    @ParameterizedTest
    @EnumSource(
            value = InvitationStatus.class,
            names = {"ACCEPTED", "REJECTED", "CANCELED"}
    )
    void cancel_alreadyProcessed(InvitationStatus status) {
        Project project = project(10L);
        Member ownerMember = member(1L, "owner@example.com", "owner");
        ProjectMember owner = projectMember(ownerMember, project, ProjectMemberRole.OWNER);
        Invitation invitation = invitation(
                100L,
                project,
                ownerMember,
                member(2L, "invitee@example.com", "invitee"),
                LocalDateTime.of(2026, 8, 22, 10, 0)
        );
        ReflectionTestUtils.setField(invitation, "status", status);
        givenProjectAndRequester(project, owner);
        given(invitationRepository.findByIdForUpdate(100L))
                .willReturn(Optional.of(invitation));

        assertError(
                () -> invitationService.cancel(1L, 10L, 100L),
                ErrorCode.INVITATION_ALREADY_PROCESSED
        );

        assertThat(invitation.getStatus()).isEqualTo(status);
    }

    private Invitation invitationForAccept() {
        return invitation(
                100L,
                project(10L),
                member(1L, "owner@example.com", "owner"),
                member(2L, "invitee@example.com", "invitee"),
                LocalDateTime.of(2026, 8, 22, 10, 0)
        );
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
