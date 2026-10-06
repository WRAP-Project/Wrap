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
import com.wrap.domain.projectmember.service.ProjectMemberCapacityValidator;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.Clock;
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
    private final ProjectMemberCapacityValidator projectMemberCapacityValidator;
    private final Clock clock;

    @Transactional
    public List<ReceivedInvitationResponse> getReceivedInvitations(Long memberId) {
        LocalDateTime now = now();
        List<Invitation> invitations =
                invitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(memberId);
        invitations.forEach(invitation -> invitation.expireIfNeeded(now));

        return invitations.stream()
                .filter(invitation -> invitation.getStatus() == InvitationStatus.INVITED)
                .map(ReceivedInvitationResponse::from)
                .toList();
    }

    @Transactional
    public List<InvitationResponse> getSentInvitations(Long memberId, Long projectId) {
        findProjectForOwner(memberId, projectId);

        LocalDateTime now = now();
        List<Invitation> invitations =
                invitationRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
        invitations.forEach(invitation -> invitation.expireIfNeeded(now));

        return invitations.stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @Transactional
    public InvitationResponse create(
            Long memberId,
            Long projectId,
            InvitationCreateRequest request
    ) {
        Project project = findActiveProjectForUpdate(projectId);
        ProjectMember inviterProjectMember = findJoinedMember(memberId, projectId);
        validateOwner(inviterProjectMember);
        validateProjectInProgress(project);

        LocalDateTime issuedAt = now();
        Member invitee = findActiveMemberByEmail(request.getEmail().trim());
        validateNotJoinedMember(invitee.getId(), projectId);
        validateNoActiveInvitation(projectId, invitee.getId(), issuedAt);
        projectMemberCapacityValidator.validateSeatAvailable(projectId, issuedAt);

        Invitation invitation = Invitation.create(
                project,
                inviterProjectMember.getMember(),
                invitee,
                request.getRole(),
                issuedAt
        );

        return InvitationResponse.from(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationResponse accept(Long memberId, Long invitationId) {
        Invitation invitation = findPendingInvitationForInviteeWithProjectLock(
                memberId,
                invitationId
        );
        joinProject(invitation);
        invitation.accept();

        return InvitationResponse.from(invitation);
    }

    @Transactional
    public InvitationResponse reject(Long memberId, Long invitationId) {
        Invitation invitation = findPendingInvitationForInvitee(memberId, invitationId);
        invitation.reject();

        return InvitationResponse.from(invitation);
    }

    @Transactional
    public void cancel(Long memberId, Long projectId, Long invitationId) {
        Project project = findProjectForOwner(memberId, projectId);
        validateProjectInProgress(project);

        Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));
        validateInvitationProject(projectId, invitation);
        validateInvitationNotExpired(invitation);
        validateInvitationPending(invitation);
        invitation.cancel();
    }

    private Project findProjectForOwner(Long memberId, Long projectId) {
        Project project = findActiveProject(projectId);
        ProjectMember projectMember = findJoinedMember(memberId, projectId);
        validateOwner(projectMember);
        return project;
    }

    private Invitation findPendingInvitationForInvitee(Long memberId, Long invitationId) {
        Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));
        validateInvitee(memberId, invitation);
        validateInvitationNotExpired(invitation);
        validateInvitationPending(invitation);
        validateProjectAvailable(invitation.getProject());
        return invitation;
    }

    private Invitation findPendingInvitationForInviteeWithProjectLock(
            Long memberId,
            Long invitationId
    ) {
        Long projectId = invitationRepository.findProjectIdById(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedAtIsNullForUpdate(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));
        validateInvitee(memberId, invitation);
        validateInvitationNotExpired(invitation);
        validateInvitationPending(invitation);
        validateProjectInProgress(project);
        return invitation;
    }

    private void validateInvitationProject(Long projectId, Invitation invitation) {
        if (!invitation.getProject().getId().equals(projectId)) {
            throw new CustomException(ErrorCode.INVITATION_NOT_FOUND);
        }
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

    private void validateInvitationNotExpired(Invitation invitation) {
        if (invitation.getStatus() == InvitationStatus.EXPIRED
                || (invitation.getStatus() == InvitationStatus.INVITED
                && invitation.isExpiredAt(now()))) {
            throw new CustomException(ErrorCode.INVITATION_EXPIRED);
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
        LocalDateTime joinedAt = now();

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

    private Project findActiveProjectForUpdate(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNullForUpdate(projectId)
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

    private void validateNoActiveInvitation(
            Long projectId,
            Long inviteeId,
            LocalDateTime now
    ) {
        List<Invitation> invitations = invitationRepository
                .findAllByProjectIdAndInviteeIdAndStatus(
                        projectId,
                        inviteeId,
                        InvitationStatus.INVITED
                );
        invitations.forEach(invitation -> invitation.expireIfNeeded(now));

        boolean hasActiveInvitation = invitations.stream()
                .anyMatch(invitation -> invitation.getStatus() == InvitationStatus.INVITED);

        if (hasActiveInvitation) {
            throw new CustomException(ErrorCode.INVITATION_ALREADY_EXISTS);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
