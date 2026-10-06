package com.wrap.domain.projectmember.service;

import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.invitation.repository.InvitationRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMemberCapacityValidator {

    public static final int MAX_CAPACITY = 6;

    private final ProjectMemberRepository projectMemberRepository;
    private final InvitationRepository invitationRepository;

    public void validateSeatAvailable(Long projectId, LocalDateTime now) {
        long joinedMemberCount = projectMemberRepository.countByProjectIdAndStatus(
                projectId,
                ProjectMemberStatus.JOINED
        );
        long validInvitationCount = invitationRepository
                .countByProjectIdAndStatusAndExpiresAtAfter(
                        projectId,
                        InvitationStatus.INVITED,
                        now
                );

        if (joinedMemberCount + validInvitationCount >= MAX_CAPACITY) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
        }
    }
}
