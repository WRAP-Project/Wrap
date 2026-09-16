package com.wrap.domain.project.dto.response;

import java.util.List;

public record ProjectReportResponse(
        int percent,
        long doneCount,
        long inProgressCount,
        long needsCheckCount,
        List<ProjectReportAreaResponse> areas,
        List<ProjectReportRiskResponse> risks
) {
}
