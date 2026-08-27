package com.wrap.domain.invitation.service;

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
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final InvitationRepository invitationRepository;

    @Transactional(readOnly = true)
    public List<ReceivedInvitationResponse> getReceivedInvitations(Long memberId) {
        return invitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(ReceivedInvitationResponse::from)
                .toList();
    }

    @Transactional
    public InvitationResponse create(
            Long memberId,
            Long projectId,
            InvitationCreateRequest request
    ) {
        Project project = findActiveProject(projectId);
        ProjectMember inviterProjectMember = findJoinedMember(memberId, projectId);
        validateOwner(inviterProjectMember);
        validateProjectInProgress(project);

        Member invitee = findActiveMemberByEmail(request.getEmail().trim());
        validateNotJoinedMember(invitee.getId(), projectId);
        validateNoActiveInvitation(projectId, invitee.getId());

        Invitation invitation = Invitation.create(
                project,
                inviterProjectMember.getMember(),
                invitee,
                request.getRole()
        );

        return InvitationResponse.from(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationResponse accept(Long memberId, Long invitationId) {
        Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));

        validateInvitee(memberId, invitation);
        validateInvitationPending(invitation);
        validateProjectAvailable(invitation.getProject());
        joinProject(invitation);
        invitation.accept();

        return InvitationResponse.from(invitation);
    }

    private void validateInvitee(Long memberId, Invitation invitation) {
        if (!invitation.getInvitee().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.INVITATION_ACCESS_DENIED);
        }
    }

    private void validateInvitationPending(Invitation invitation) {
        if (invitation.getStatus() != InvitationStatus.INVITED) {
            throw new CustomException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }
    }

    private void validateProjectAvailable(Project project) {
        if (project.isDeleted()) {
            throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
        }
        validateProjectInProgress(project);
    }

    private void joinProject(Invitation invitation) {
        Project project = invitation.getProject();
        Member invitee = invitation.getInvitee();
        LocalDateTime joinedAt = LocalDateTime.now();

        projectMemberRepository.findByMemberIdAndProjectId(invitee.getId(), project.getId())
                .ifPresentOrElse(
                        projectMember -> {
                            if (projectMember.getStatus() != ProjectMemberStatus.LEFT) {
                                throw new CustomException(
                                        ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS
                                );
                            }
                            projectMember.rejoin(invitation.getRole(), joinedAt);
                        },
                        () -> projectMemberRepository.save(ProjectMember.join(
                                invitee,
                                project,
                                invitation.getRole(),
                                joinedAt
                        ))
                );
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember findJoinedMember(Long memberId, Long projectId) {
        return projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                        memberId,
                        projectId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_ACCESS_DENIED));
    }

    private void validateOwner(ProjectMember projectMember) {
        if (!projectMember.isOwner()) {
            throw new CustomException(ErrorCode.PROJECT_OWNER_REQUIRED);
        }
    }

    private void validateProjectInProgress(Project project) {
        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }
    }

    private Member findActiveMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .filter(member -> member.getDeletedAt() == null)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateNotJoinedMember(Long inviteeId, Long projectId) {
        if (projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                inviteeId,
                projectId,
                ProjectMemberStatus.JOINED
        )) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }
    }

    private void validateNoActiveInvitation(Long projectId, Long inviteeId) {
        if (invitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                projectId,
                inviteeId,
                InvitationStatus.INVITED
        )) {
            throw new CustomException(ErrorCode.INVITATION_ALREADY_EXISTS);
        }
    }
}
