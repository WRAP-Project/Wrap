package com.wrap.domain.invitation.dto.response;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationResponse {

    private Long invitationId;
    private Long projectId;
    private String projectName;
    private Long inviteeMemberId;
    private String inviteeEmail;
    private ProjectMemberRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    public static InvitationResponse from(Invitation invitation) {
        return InvitationResponse.builder()
                .invitationId(invitation.getId())
                .projectId(invitation.getProject().getId())
                .projectName(invitation.getProject().getName())
                .inviteeMemberId(invitation.getInvitee().getId())
                .inviteeEmail(invitation.getInvitee().getEmail())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
