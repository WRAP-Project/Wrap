package com.wrap.domain.project.controller;

import com.wrap.domain.project.dto.response.ProjectReportResponse;
import com.wrap.domain.project.service.ProjectReportService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectReportController {

    private final ProjectReportService projectReportService;

    @GetMapping("/projects/{projectId}/report")
    public ApiResponse<ProjectReportResponse> getReport(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectReportService.getReport(memberDetails.getMemberId(), projectId),
                "Project report retrieved."
        );
    }
}
