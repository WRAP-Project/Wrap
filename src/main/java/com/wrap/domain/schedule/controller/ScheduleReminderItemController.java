package com.wrap.domain.schedule.controller;

import com.wrap.domain.schedule.dto.ReminderItemStatusResponse;
import com.wrap.domain.schedule.dto.ReminderItemStatusUpdateRequest;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistSourceType;
import com.wrap.domain.schedule.service.ScheduleReminderItemService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScheduleReminderItemController {

    private final ScheduleReminderItemService scheduleReminderItemService;

    @PatchMapping("/projects/{projectId}/schedules/{scheduleId}/reminder-items/{sourceType}/{sourceId}/status")
    public ApiResponse<ReminderItemStatusResponse> updateStatus(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long scheduleId,
            @PathVariable ReminderChecklistSourceType sourceType,
            @PathVariable Long sourceId,
            @Valid @RequestBody ReminderItemStatusUpdateRequest request
    ) {
        return ApiResponse.success(
                scheduleReminderItemService.updateStatus(
                        memberDetails.getMemberId(),
                        projectId,
                        scheduleId,
                        sourceType,
                        sourceId,
                        request
                ),
                "Reminder item status updated."
        );
    }
}
