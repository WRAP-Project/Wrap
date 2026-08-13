package com.wrap.domain.availability.controller;

import com.wrap.domain.availability.dto.AvailabilityConfirmRequest;
import com.wrap.domain.availability.dto.AvailabilityRequestCreateRequest;
import com.wrap.domain.availability.dto.AvailabilityRequestDetailResponse;
import com.wrap.domain.availability.dto.AvailabilityRequestSummaryResponse;
import com.wrap.domain.availability.dto.AvailabilityResponseUpsertRequest;
import com.wrap.domain.availability.dto.AvailabilityResponsesResponse;
import com.wrap.domain.availability.dto.MyAvailabilityResponse;
import com.wrap.domain.availability.dto.RecommendedSlotResponse;
import com.wrap.domain.availability.enums.AvailabilityRequestStatus;
import com.wrap.domain.availability.service.AvailabilityService;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Availability", description = "Team availability request APIs")
@RestController
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @Operation(summary = "Create availability request")
    @PostMapping("/projects/{projectId}/availability-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AvailabilityRequestDetailResponse> create(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody AvailabilityRequestCreateRequest request
    ) {
        return ApiResponse.success(
                availabilityService.create(memberDetails.getMemberId(), projectId, request),
                "Availability request created."
        );
    }

    @Operation(summary = "List availability requests")
    @GetMapping("/projects/{projectId}/availability-requests")
    public ApiResponse<List<AvailabilityRequestSummaryResponse>> findAll(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) AvailabilityRequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(
                availabilityService.findAll(memberDetails.getMemberId(), projectId, status, from, to),
                "Availability requests retrieved."
        );
    }

    @Operation(summary = "Get availability request detail")
    @GetMapping("/projects/{projectId}/availability-requests/{availabilityRequestId}")
    public ApiResponse<AvailabilityRequestDetailResponse> findDetail(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long availabilityRequestId
    ) {
        return ApiResponse.success(
                availabilityService.findDetail(memberDetails.getMemberId(), projectId, availabilityRequestId),
                "Availability request retrieved."
        );
    }

    @Operation(summary = "Get my availability response")
    @GetMapping("/projects/{projectId}/availability-requests/{availabilityRequestId}/me/response")
    public ApiResponse<MyAvailabilityResponse> findMyResponse(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long availabilityRequestId
    ) {
        return ApiResponse.success(
                availabilityService.findMyResponse(memberDetails.getMemberId(), projectId, availabilityRequestId),
                "My availability response retrieved."
        );
    }

    @Operation(summary = "Submit my availability response")
    @PutMapping("/projects/{projectId}/availability-requests/{availabilityRequestId}/me/response")
    public ApiResponse<MyAvailabilityResponse> upsertMyResponse(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long availabilityRequestId,
            @Valid @RequestBody AvailabilityResponseUpsertRequest request
    ) {
        return ApiResponse.success(
                availabilityService.upsertMyResponse(
                        memberDetails.getMemberId(),
                        projectId,
                        availabilityRequestId,
                        request
                ),
                "My availability response saved."
        );
    }

    @Operation(summary = "Get member availability responses")
    @GetMapping("/projects/{projectId}/availability-requests/{availabilityRequestId}/responses")
    public ApiResponse<AvailabilityResponsesResponse> findResponses(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long availabilityRequestId
    ) {
        return ApiResponse.success(
                availabilityService.findResponses(memberDetails.getMemberId(), projectId, availabilityRequestId),
                "Availability responses retrieved."
        );
    }

    @Operation(summary = "Get recommended availability slots")
    @GetMapping("/projects/{projectId}/availability-requests/{availabilityRequestId}/recommended-slots")
    public ApiResponse<List<RecommendedSlotResponse>> findRecommendedSlots(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long availabilityRequestId
    ) {
        return ApiResponse.success(
                availabilityService.findRecommendedSlots(
                        memberDetails.getMemberId(),
                        projectId,
                        availabilityRequestId
                ),
                "Recommended slots retrieved."
        );
    }

    @Operation(summary = "Confirm availability request")
    @PostMapping("/projects/{projectId}/availability-requests/{availabilityRequestId}/confirm")
    public ApiResponse<ScheduleResponse> confirm(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long availabilityRequestId,
            @Valid @RequestBody AvailabilityConfirmRequest request
    ) {
        return ApiResponse.success(
                availabilityService.confirm(memberDetails.getMemberId(), projectId, availabilityRequestId, request),
                "Availability request confirmed."
        );
    }
}
