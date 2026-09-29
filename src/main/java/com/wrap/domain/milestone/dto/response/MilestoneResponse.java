package com.wrap.domain.milestone.dto.response;

import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.enums.MilestoneStatus;
import java.time.LocalDate;

public record MilestoneResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        LocalDate dueDate,
        MilestoneStatus status,
        int totalTaskCount,
        int doneTaskCount
) {

    public static MilestoneResponse of(Milestone milestone, int totalTaskCount, int doneTaskCount) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getProject().getId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getDueDate(),
                milestone.getStatus(),
                totalTaskCount,
                doneTaskCount
        );
    }

    public static MilestoneResponse from(Milestone milestone) {
        return of(milestone, 0, 0);
    }
}
