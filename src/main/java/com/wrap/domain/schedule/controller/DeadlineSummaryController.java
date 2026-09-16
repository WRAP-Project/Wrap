package com.wrap.domain.schedule.controller;

import com.wrap.domain.schedule.dto.DeadlineSummaryResponse;
import com.wrap.domain.schedule.service.DeadlineSummaryService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeadlineSummaryController {

    private final DeadlineSummaryService deadlineSummaryService;

    @GetMapping("/projects/{projectId}/deadline-summary")
    public ApiResponse<List<DeadlineSummaryResponse>> getDeadlineSummary(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(
                deadlineSummaryService.getSummaries(memberDetails.getMemberId(), projectId, limit),
                "Deadline summaries retrieved."
        );
    }
}
