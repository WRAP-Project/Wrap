package com.wrap.domain.invitelink.service;

import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkInfoResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteJoinResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkSummaryResponse;
import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import com.wrap.domain.invitelink.repository.ProjectInviteLinkRepository;
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
import com.wrap.domain.projectmember.service.ProjectMemberCapacityValidator;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectInviteLinkService {

    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectInviteLinkRepository inviteLinkRepository;
    private final InvitationRepository invitationRepository;
    private final ProjectMemberValidator projectMemberValidator;
    private final ProjectMemberCapacityValidator projectMemberCapacityValidator;
    private final InviteTokenGenerator tokenGenerator;
    private final Clock clock;
    private final String inviteBaseUrl;

    public ProjectInviteLinkService(
            ProjectRepository projectRepository,
            MemberRepository memberRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectInviteLinkRepository inviteLinkRepository,
            InvitationRepository invitationRepository,
            ProjectMemberValidator projectMemberValidator,
            ProjectMemberCapacityValidator projectMemberCapacityValidator,
            InviteTokenGenerator tokenGenerator,
            Clock clock,
            @Value("${app.invite-link.base-url}") String inviteBaseUrl
    ) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.inviteLinkRepository = inviteLinkRepository;
        this.invitationRepository = invitationRepository;
        this.projectMemberValidator = projectMemberValidator;
        this.projectMemberCapacityValidator = projectMemberCapacityValidator;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.inviteBaseUrl = normalizeBaseUrl(inviteBaseUrl);
    }

    @Transactional
    public ProjectInviteLinkResponse create(Long memberId, Long projectId) {
        ProjectMember creator = projectMemberValidator.findJoinedMember(memberId, projectId);
        projectMemberValidator.validateOwner(creator);

        Project project = projectRepository.findByIdAndDeletedAtIsNullForUpdate(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        validateProjectInProgress(project);
        LocalDateTime now = now();
        validateNoActiveInviteLink(projectId, now);

        GeneratedInviteToken generatedToken = generateUniqueToken();
        ProjectInviteLink inviteLink = inviteLinkRepository.save(ProjectInviteLink.create(
                project,
                creator.getMember(),
                generatedToken.tokenHash(),
                now
        ));

        return ProjectInviteLinkResponse.from(
                inviteLink,
                inviteBaseUrl + "/" + generatedToken.rawToken()
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectInviteLinkSummaryResponse> getInviteLinks(
            Long memberId,
            Long projectId
    ) {
        ProjectMember requester = projectMemberValidator.findJoinedMember(memberId, projectId);
        projectMemberValidator.validateOwner(requester);

        return inviteLinkRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(ProjectInviteLinkSummaryResponse::from)
                .toList();
    }

    @Transactional
    public void revoke(Long memberId, Long projectId, Long inviteLinkId) {
        ProjectMember requester = projectMemberValidator.findJoinedMember(memberId, projectId);
        projectMemberValidator.validateOwner(requester);

        Project project = projectRepository.findByIdAndDeletedAtIsNullForUpdate(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
        validateProjectInProgress(project);

        ProjectInviteLink inviteLink = inviteLinkRepository.findByIdAndProjectIdForUpdate(
                inviteLinkId,
                projectId
        ).orElseThrow(() -> new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND));
        if (!inviteLink.isActive()) {
            throw new CustomException(ErrorCode.INVITE_LINK_ALREADY_REVOKED);
        }

        inviteLink.revoke(now());
    }

    @Transactional(readOnly = true)
    public ProjectInviteLinkInfoResponse getInviteLinkInfo(String rawToken) {
        String tokenHash = tokenGenerator.hash(rawToken);
        ProjectInviteLink inviteLink = inviteLinkRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND));
        validateInviteLinkUsable(inviteLink);
        validateProjectAvailable(inviteLink.getProject());

        return ProjectInviteLinkInfoResponse.from(inviteLink);
    }

    @Transactional
    public ProjectInviteJoinResponse join(Long memberId, String rawToken) {
        String tokenHash = tokenGenerator.hash(rawToken);
        Long projectId = inviteLinkRepository.findProjectIdByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedAtIsNullForUpdate(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND));
        ProjectInviteLink inviteLink = inviteLinkRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND));
        validateInviteLinkUsable(inviteLink);
        validateProjectAvailable(project);

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        ProjectMember projectMember = joinProject(member, project);

        return ProjectInviteJoinResponse.from(projectMember);
    }

    private ProjectMember joinProject(Member member, Project project) {
        LocalDateTime joinedAt = now();
        ProjectMember existingProjectMember = projectMemberRepository
                .findByMemberIdAndProjectId(member.getId(), project.getId())
                .orElse(null);
        if (existingProjectMember != null
                && existingProjectMember.getStatus() != ProjectMemberStatus.LEFT) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS);
        }

        Invitation activeInvitation = invitationRepository
                .findActiveByProjectIdAndInviteeIdForUpdate(
                        project.getId(),
                        member.getId(),
                        InvitationStatus.INVITED,
                        joinedAt
                )
                .orElse(null);
        ProjectMemberRole role = activeInvitation == null
                ? ProjectMemberRole.MEMBER
                : activeInvitation.getRole();

        if (activeInvitation == null) {
            projectMemberCapacityValidator.validateSeatAvailable(project.getId(), joinedAt);
        }

        ProjectMember projectMember;
        if (existingProjectMember == null) {
            projectMember = projectMemberRepository.save(ProjectMember.join(
                    member,
                    project,
                    role,
                    joinedAt
            ));
        } else {
            existingProjectMember.rejoin(role, joinedAt);
            projectMember = existingProjectMember;
        }

        if (activeInvitation != null) {
            activeInvitation.accept();
        }
        return projectMember;
    }

    private GeneratedInviteToken generateUniqueToken() {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            GeneratedInviteToken generatedToken = tokenGenerator.generate();
            if (!inviteLinkRepository.existsByTokenHash(generatedToken.tokenHash())) {
                return generatedToken;
            }
        }

        throw new CustomException(ErrorCode.INVITE_LINK_TOKEN_GENERATION_FAILED);
    }

    private void validateNoActiveInviteLink(Long projectId, LocalDateTime now) {
        if (inviteLinkRepository.existsByProjectIdAndActiveTrueAndExpiresAtAfter(
                projectId,
                now
        )) {
            throw new CustomException(ErrorCode.INVITE_LINK_ALREADY_EXISTS);
        }
    }

    private void validateInviteLinkUsable(ProjectInviteLink inviteLink) {
        if (!inviteLink.isActive()) {
            throw new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND);
        }
        if (inviteLink.isExpiredAt(now())) {
            throw new CustomException(ErrorCode.INVITE_LINK_EXPIRED);
        }
    }

    private void validateProjectInProgress(Project project) {
        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }
    }

    private void validateProjectAvailable(Project project) {
        if (project.isDeleted()) {
            throw new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND);
        }
        validateProjectInProgress(project);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("초대 링크 기본 URL은 필수입니다.");
        }
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
