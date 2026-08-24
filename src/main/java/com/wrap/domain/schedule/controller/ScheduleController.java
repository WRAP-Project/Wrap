package com.wrap.domain.schedule.controller;

import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleDetailResponse;
import com.wrap.domain.schedule.dto.ScheduleReminderResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.service.ScheduleService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> create(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody ScheduleCreateRequest request
    ) {
        return ApiResponse.success(
                scheduleService.create(memberDetails.getMemberId(), request),
                "Schedule created."
        );
    }

    @GetMapping("/schedules/me")
    public ApiResponse<List<ScheduleResponse>> findMine(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long projectId
    ) {
        return ApiResponse.success(
                scheduleService.findMine(memberDetails.getMemberId(), from, to, projectId),
                "My schedules retrieved."
        );
    }

    @GetMapping("/projects/{projectId}/schedules")
    public ApiResponse<List<ScheduleResponse>> findProjectSchedules(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(
                scheduleService.findProjectSchedules(memberDetails.getMemberId(), projectId, from, to),
                "Project schedules retrieved."
        );
    }

    @GetMapping("/schedules/{scheduleId}")
    public ApiResponse<ScheduleDetailResponse> findDetail(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long scheduleId
    ) {
        return ApiResponse.success(
                scheduleService.findDetail(memberDetails.getMemberId(), scheduleId),
                "Schedule detail retrieved."
        );
    }

    @PatchMapping("/schedules/{scheduleId}")
    public ApiResponse<ScheduleResponse> update(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        return ApiResponse.success(
                scheduleService.update(memberDetails.getMemberId(), scheduleId, request),
                "Schedule updated."
        );
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long scheduleId
    ) {
        scheduleService.delete(memberDetails.getMemberId(), scheduleId);
        return ApiResponse.success("Schedule deleted.");
    }

    @PatchMapping("/schedules/{scheduleId}/check")
    public ApiResponse<ScheduleDetailResponse> check(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long scheduleId
    ) {
        return ApiResponse.success(
                scheduleService.check(memberDetails.getMemberId(), scheduleId),
                "Schedule checked."
        );
    }

    @PatchMapping("/schedules/{scheduleId}/uncheck")
    public ApiResponse<ScheduleDetailResponse> uncheck(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long scheduleId
    ) {
        return ApiResponse.success(
                scheduleService.uncheck(memberDetails.getMemberId(), scheduleId),
                "Schedule unchecked."
        );
    }

    @GetMapping("/projects/{projectId}/schedules/reminders")
    public ApiResponse<List<ScheduleReminderResponse>> findReminders(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiResponse.success(
                scheduleService.findReminders(memberDetails.getMemberId(), projectId, days, limit),
                "Schedule reminders retrieved."
        );
    }
}
