package com.wrap.domain.chat.dto.response;

import com.wrap.domain.projectmember.entity.ProjectMember;

public record ChatMemberResponse(
        Long projectMemberId,
        String nickname,
        String profileImage
) {

    public static ChatMemberResponse from(ProjectMember projectMember) {
        return new ChatMemberResponse(
                projectMember.getId(),
                projectMember.getMember().getNickname(),
                projectMember.getMember().getProfileImage()
        );
    }
}
