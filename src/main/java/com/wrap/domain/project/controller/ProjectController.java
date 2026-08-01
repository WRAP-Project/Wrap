package com.wrap.domain.project.controller;

import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.request.ProjectUpdateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.dto.response.ProjectSummaryResponse;
import com.wrap.domain.project.service.ProjectService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project", description = "프로젝트 관련 API")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectResponse> create(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        return ApiResponse.success(
                projectService.create(memberDetails.getMemberId(), request),
                "Project created."
        );
    }

    @Operation(summary = "내 프로젝트 목록 조회")
    @GetMapping
    public ApiResponse<List<ProjectSummaryResponse>> getMyProjects(
            @AuthenticationPrincipal MemberDetails memberDetails
    ) {
        return ApiResponse.success(
                projectService.getMyProjects(memberDetails.getMemberId()),
                "My projects retrieved."
        );
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectService.getProject(memberDetails.getMemberId(), projectId),
                "Project retrieved."
        );
    }

    @Operation(summary = "프로젝트 정보 수정")
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResponse> update(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        return ApiResponse.success(
                projectService.update(memberDetails.getMemberId(), projectId, request),
                "Project updated."
        );
    }

    @Operation(summary = "프로젝트 완료")
    @PatchMapping("/{projectId}/complete")
    public ApiResponse<ProjectResponse> complete(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectService.complete(memberDetails.getMemberId(), projectId),
                "Project completed."
        );
    }

    @Operation(summary = "프로젝트 재진행")
    @PatchMapping("/{projectId}/reopen")
    public ApiResponse<ProjectResponse> reopen(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectService.reopen(memberDetails.getMemberId(), projectId),
                "Project reopened."
        );
    }
}
