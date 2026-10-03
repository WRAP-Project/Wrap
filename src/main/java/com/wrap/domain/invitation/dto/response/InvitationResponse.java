package com.wrap.domain.invitation.dto.response;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "보낸 프로젝트 초대 응답")
public class InvitationResponse {

    private Long invitationId;
    private Long projectId;
    private String projectName;
    private Long inviteeMemberId;
    private String inviteeEmail;
    private ProjectMemberRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    @Schema(description = "초대 만료 시각", example = "2026-08-25T10:00:00")
    private LocalDateTime expiresAt;

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
                .expiresAt(invitation.getExpiresAt())
                .build();
    }
}
