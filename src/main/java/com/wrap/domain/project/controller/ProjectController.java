package com.wrap.domain.project.controller;

import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.service.ProjectService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
