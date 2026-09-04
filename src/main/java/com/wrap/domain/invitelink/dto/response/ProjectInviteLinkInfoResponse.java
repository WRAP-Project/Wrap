package com.wrap.domain.invitelink.dto.response;

import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공개 초대 링크 프로젝트 정보 응답")
public class ProjectInviteLinkInfoResponse {

    @Schema(description = "프로젝트 ID", example = "10")
    private Long projectId;

    @Schema(description = "프로젝트 이름", example = "WRAP")
    private String projectName;

    @Schema(description = "프로젝트 대표 색상", example = "#CDEA6F")
    private String projectColor;

    @Schema(description = "초대 링크를 생성한 회원 닉네임", example = "홍길동")
    private String inviterNickname;

    public static ProjectInviteLinkInfoResponse from(ProjectInviteLink inviteLink) {
        return ProjectInviteLinkInfoResponse.builder()
                .projectId(inviteLink.getProject().getId())
                .projectName(inviteLink.getProject().getName())
                .projectColor(inviteLink.getProject().getColor())
                .inviterNickname(inviteLink.getCreatedBy().getNickname())
                .build();
    }
}
