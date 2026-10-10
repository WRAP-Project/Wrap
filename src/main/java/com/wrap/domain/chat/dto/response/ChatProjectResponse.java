package com.wrap.domain.chat.dto.response;

import com.wrap.domain.project.entity.Project;

public record ChatProjectResponse(
        Long projectId,
        String name,
        String color
) {

    public static ChatProjectResponse from(Project project) {
        return new ChatProjectResponse(project.getId(), project.getName(), project.getColor());
    }
}
