package com.wrap.domain.project.dto.response;

public record ProjectReportRiskResponse(
        Long taskId,
        String title,
        String detail
) {
}
