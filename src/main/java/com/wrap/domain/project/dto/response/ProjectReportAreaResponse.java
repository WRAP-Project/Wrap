package com.wrap.domain.project.dto.response;

public record ProjectReportAreaResponse(
        String area,
        int percent,
        boolean delayed,
        String note
) {
}
