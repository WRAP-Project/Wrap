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
@Schema(description = "받은 프로젝트 초대 응답")
public class ReceivedInvitationResponse {

    private Long invitationId;
    private Long projectId;
    private String projectName;
    private String inviterNickname;
    private ProjectMemberRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    @Schema(description = "초대 만료 시각", example = "2026-08-25T10:00:00")
    private LocalDateTime expiresAt;

    public static ReceivedInvitationResponse from(Invitation invitation) {
        return ReceivedInvitationResponse.builder()
                .invitationId(invitation.getId())
                .projectId(invitation.getProject().getId())
                .projectName(invitation.getProject().getName())
                .inviterNickname(invitation.getInviter().getNickname())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .build();
    }
}
