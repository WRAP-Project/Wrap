package com.wrap.domain.milestone.controller;

import com.wrap.domain.milestone.dto.request.MilestoneCreateRequest;
import com.wrap.domain.milestone.dto.request.MilestoneUpdateRequest;
import com.wrap.domain.milestone.dto.response.MilestoneResponse;
import com.wrap.domain.milestone.service.MilestoneService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Milestone", description = "마일스톤 관련 API")
@RestController
@RequestMapping("/projects/{projectId}/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @Operation(summary = "마일스톤 목록 조회")
    @GetMapping
    public ApiResponse<List<MilestoneResponse>> findProjectMilestones(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                milestoneService.findProjectMilestones(memberDetails.getMemberId(), projectId),
                "Milestones retrieved."
        );
    }

    @Operation(summary = "마일스톤 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MilestoneResponse> create(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody MilestoneCreateRequest request
    ) {
        return ApiResponse.success(
                milestoneService.create(memberDetails.getMemberId(), projectId, request),
                "Milestone created."
        );
    }

    @Operation(summary = "마일스톤 수정")
    @PatchMapping("/{milestoneId}")
    public ApiResponse<MilestoneResponse> update(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request
    ) {
        return ApiResponse.success(
                milestoneService.update(memberDetails.getMemberId(), projectId, milestoneId, request),
                "Milestone updated."
        );
    }

    @Operation(summary = "마일스톤 삭제")
    @DeleteMapping("/{milestoneId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long milestoneId
    ) {
        milestoneService.delete(memberDetails.getMemberId(), projectId, milestoneId);
        return ApiResponse.success("Milestone deleted.");
    }
}
