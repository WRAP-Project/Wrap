package com.wrap.domain.milestone.dto.response;

import com.wrap.domain.milestone.entity.Milestone;
import java.time.LocalDate;

public record MilestoneResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        LocalDate dueDate
) {

    public static MilestoneResponse from(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getProject().getId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getDueDate()
        );
    }
}
