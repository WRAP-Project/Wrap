package com.wrap.domain.calendar.controller;

import com.wrap.domain.calendar.dto.CalendarRiskCheckResponse;
import com.wrap.domain.calendar.service.CalendarService;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/projects/{projectId}/calendar/risk-checks")
    public ApiResponse<List<CalendarRiskCheckResponse>> findRiskChecks(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeProjectMemberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo
    ) {
        return ApiResponse.success(
                calendarService.findRiskChecks(
                        memberDetails.getMemberId(),
                        projectId,
                        status,
                        assigneeProjectMemberId,
                        dueFrom,
                        dueTo
                ),
                "Calendar risk checks retrieved."
        );
    }
}
