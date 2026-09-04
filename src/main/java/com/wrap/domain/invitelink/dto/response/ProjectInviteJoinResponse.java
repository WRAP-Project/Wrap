package com.wrap.domain.invitelink.dto.response;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "초대 링크 프로젝트 참여 응답")
public class ProjectInviteJoinResponse {

    @Schema(description = "참여한 프로젝트 ID", example = "10")
    private Long projectId;

    @Schema(description = "참여한 프로젝트 이름", example = "WRAP")
    private String projectName;

    @Schema(description = "프로젝트 멤버 ID", example = "25")
    private Long projectMemberId;

    @Schema(description = "참여한 회원 ID", example = "7")
    private Long memberId;

    @Schema(
            description = "프로젝트 역할. 링크 참여자는 MEMBER로 설정됩니다.",
            example = "MEMBER"
    )
    private ProjectMemberRole role;

    @Schema(description = "프로젝트 참여 상태", example = "JOINED")
    private ProjectMemberStatus status;

    @Schema(description = "프로젝트 참여 시각", example = "2026-09-02T10:30:00")
    private LocalDateTime joinedAt;

    public static ProjectInviteJoinResponse from(ProjectMember projectMember) {
        return ProjectInviteJoinResponse.builder()
                .projectId(projectMember.getProject().getId())
                .projectName(projectMember.getProject().getName())
                .projectMemberId(projectMember.getId())
                .memberId(projectMember.getMember().getId())
                .role(projectMember.getRole())
                .status(projectMember.getStatus())
                .joinedAt(projectMember.getJoinedAt())
                .build();
    }
}
