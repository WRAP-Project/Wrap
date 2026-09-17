package com.wrap.domain.project.controller;

import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.request.ProjectUpdateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.dto.response.ProjectSummaryResponse;
import com.wrap.domain.project.service.ProjectService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Operation(
            summary = "프로젝트 정보 부분 수정",
            description = """
                    로그인한 사용자 중 프로젝트에 참여 중인(JOINED) OWNER만 수정할 수 있습니다.
                    모든 필드는 선택이며, 생략하거나 null을 보내면 기존 값을 유지합니다.
                    description, goal, successCriteria는 빈 문자열 또는 공백만 보내면 내용을 삭제합니다.
                    name과 color는 빈 문자열 또는 공백만 보낼 수 없습니다.
                    날짜는 기존 값과 요청 값을 합쳐 시작일이 종료일보다 늦지 않은지 검증합니다.
                    날짜는 null로 삭제할 수 없습니다. 빈 객체({})는 기존 정보를 유지합니다.
                    완료된 프로젝트는 수정할 수 없으며 409 / PROJECT_ALREADY_COMPLETED를 반환합니다.
                    수정하려면 먼저 PATCH /projects/{projectId}/reopen으로 프로젝트를 재개해야 합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "프로젝트 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력값 검증 실패(VALIDATION_FAILED), 잘못된 요청(INVALID_REQUEST) 또는 날짜 범위 오류(INVALID_DATE_RANGE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인 필요(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 참여 권한 없음(PROJECT_ACCESS_DENIED) 또는 OWNER 권한 필요(PROJECT_OWNER_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "프로젝트가 없거나 삭제됨(PROJECT_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "완료된 프로젝트는 수정 불가(PROJECT_ALREADY_COMPLETED)")
    })
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

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        projectService.delete(memberDetails.getMemberId(), projectId);
        return ApiResponse.success("Project deleted.");
    }
}
