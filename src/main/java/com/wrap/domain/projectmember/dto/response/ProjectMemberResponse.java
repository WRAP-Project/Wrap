package com.wrap.domain.projectmember.dto.response;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectMemberResponse {

    private Long projectMemberId;
    private Long memberId;
    private String nickname;
    private String profileImage;
    private ProjectMemberRole role;
    private ProjectMemberStatus status;
    private LocalDateTime joinedAt;

    public static ProjectMemberResponse from(ProjectMember projectMember) {
        return ProjectMemberResponse.builder()
                .projectMemberId(projectMember.getId())
                .memberId(projectMember.getMember().getId())
                .nickname(projectMember.getMember().getNickname())
                .profileImage(projectMember.getMember().getProfileImage())
                .role(projectMember.getRole())
                .status(projectMember.getStatus())
                .joinedAt(projectMember.getJoinedAt())
                .build();
    }
}
