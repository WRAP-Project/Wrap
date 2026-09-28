package com.wrap.domain.project.dto.response;

import com.wrap.domain.project.enums.ProjectReportAreaType;

public record ProjectReportAreaResponse(
        String area,
        ProjectReportAreaType areaType,
        int percent,
        boolean delayed,
        String note
) {
}
