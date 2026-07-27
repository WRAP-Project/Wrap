package com.wrap.domain.schedule.controller;

import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleReminderResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.service.ScheduleService;
import com.wrap.global.auth.SessionMemberResolver;
import com.wrap.global.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
    private final SessionMemberResolver sessionMemberResolver;

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> create(
            HttpSession session,
            @Valid @RequestBody ScheduleCreateRequest request
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(scheduleService.create(memberId, request), "일정이 생성되었습니다.");
    }

    @GetMapping("/schedules/me")
    public ApiResponse<List<ScheduleResponse>> findMine(
            HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long projectId
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(scheduleService.findMine(memberId, from, to, projectId), "내 일정을 조회했습니다.");
    }

    @GetMapping("/projects/{projectId}/schedules")
    public ApiResponse<List<ScheduleResponse>> findProjectSchedules(
            HttpSession session,
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                scheduleService.findProjectSchedules(memberId, projectId, from, to),
                "프로젝트 팀 일정을 조회했습니다."
        );
    }

    @PatchMapping("/schedules/{scheduleId}")
    public ApiResponse<ScheduleResponse> update(
            HttpSession session,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(scheduleService.update(memberId, scheduleId, request), "일정이 수정되었습니다.");
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ApiResponse<Void> delete(
            HttpSession session,
            @PathVariable Long scheduleId
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        scheduleService.delete(memberId, scheduleId);
        return ApiResponse.success(null, "일정이 삭제되었습니다.");
    }

    @GetMapping("/projects/{projectId}/schedules/reminders")
    public ApiResponse<List<ScheduleReminderResponse>> findReminders(
            HttpSession session,
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer limit
    ) {
        Long memberId = sessionMemberResolver.requireMemberId(session);
        return ApiResponse.success(
                scheduleService.findReminders(memberId, projectId, days, limit),
                "마감 리마인드 목록을 조회했습니다."
        );
    }
}
