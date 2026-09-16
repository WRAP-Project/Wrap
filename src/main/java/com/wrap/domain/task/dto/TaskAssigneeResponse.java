package com.wrap.domain.task.dto;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;

public record TaskAssigneeResponse(
        Long projectMemberId,
        Long memberId,
        String nickname,
        ProjectMemberRole role
) {

    public static TaskAssigneeResponse from(ProjectMember assignee) {
        if (assignee == null) {
            return null;
        }

        return new TaskAssigneeResponse(
                assignee.getId(),
                assignee.getMember().getId(),
                assignee.getMember().getNickname(),
                assignee.getRole()
        );
    }
}
