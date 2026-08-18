package com.wrap.domain.invitation.dto.response;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReceivedInvitationResponse {

    private Long invitationId;
    private Long projectId;
    private String projectName;
    private String inviterNickname;
    private ProjectMemberRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    public static ReceivedInvitationResponse from(Invitation invitation) {
        return ReceivedInvitationResponse.builder()
                .invitationId(invitation.getId())
                .projectId(invitation.getProject().getId())
                .projectName(invitation.getProject().getName())
                .inviterNickname(invitation.getInviter().getNickname())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
