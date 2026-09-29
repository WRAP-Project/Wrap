package com.wrap.domain.project.dto.response;

import com.wrap.domain.projectmember.entity.ProjectMember;

public record ProjectSummaryMemberResponse(
        Long memberId,
        String nickname,
        String profileImage
) {

    public static ProjectSummaryMemberResponse from(ProjectMember projectMember) {
        return new ProjectSummaryMemberResponse(
                projectMember.getMember().getId(),
                projectMember.getMember().getNickname(),
                projectMember.getMember().getProfileImage()
        );
    }
}
