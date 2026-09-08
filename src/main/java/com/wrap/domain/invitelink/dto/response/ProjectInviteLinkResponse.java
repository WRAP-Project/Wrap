package com.wrap.domain.invitelink.dto.response;

import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "프로젝트 초대 링크 생성 응답")
public class ProjectInviteLinkResponse {

    @Schema(description = "초대 링크 ID", example = "1")
    private Long inviteLinkId;

    @Schema(description = "프로젝트 ID", example = "10")
    private Long projectId;

    @Schema(description = "프로젝트 이름", example = "WRAP")
    private String projectName;

    @Schema(description = "초대 링크를 생성한 회원 ID", example = "3")
    private Long createdByMemberId;

    @Schema(
            description = "공유할 전체 초대 URL. 링크 생성 응답에서만 제공됩니다.",
            example = "https://wrap-client.vercel.app/join/xYz123_exampleToken"
    )
    private String inviteUrl;

    @Schema(description = "초대 링크 활성 여부", example = "true")
    private boolean active;

    @Schema(description = "초대 링크 생성 시각", example = "2026-09-02T10:00:00")
    private LocalDateTime createdAt;

    @Schema(
            description = "초대 링크 비활성화 시각. 활성 링크이면 값이 없습니다.",
            example = "2026-09-02T12:00:00"
    )
    private LocalDateTime revokedAt;

    public static ProjectInviteLinkResponse from(
            ProjectInviteLink inviteLink,
            String inviteUrl
    ) {
        return ProjectInviteLinkResponse.builder()
                .inviteLinkId(inviteLink.getId())
                .projectId(inviteLink.getProject().getId())
                .projectName(inviteLink.getProject().getName())
                .createdByMemberId(inviteLink.getCreatedBy().getId())
                .inviteUrl(inviteUrl)
                .active(inviteLink.isActive())
                .createdAt(inviteLink.getCreatedAt())
                .revokedAt(inviteLink.getRevokedAt())
                .build();
    }
}
