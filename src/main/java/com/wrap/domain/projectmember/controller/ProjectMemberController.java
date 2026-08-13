package com.wrap.domain.projectmember.controller;

import com.wrap.domain.projectmember.dto.request.ProjectMemberRoleUpdateRequest;
import com.wrap.domain.projectmember.dto.response.ProjectMemberResponse;
import com.wrap.domain.projectmember.service.ProjectMemberService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Member", description = "프로젝트 멤버 관련 API")
@RestController
@RequestMapping("/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @Operation(summary = "프로젝트 멤버 목록 조회")
    @GetMapping
    public ApiResponse<List<ProjectMemberResponse>> getProjectMembers(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                projectMemberService.getProjectMembers(
                        memberDetails.getMemberId(),
                        projectId
                ),
                "Project members retrieved."
        );
    }

    @Operation(summary = "프로젝트 멤버 역할 변경")
    @PatchMapping("/{projectMemberId}/role")
    public ApiResponse<ProjectMemberResponse> changeRole(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long projectMemberId,
            @Valid @RequestBody ProjectMemberRoleUpdateRequest request
    ) {
        return ApiResponse.success(
                projectMemberService.changeRole(
                        memberDetails.getMemberId(),
                        projectId,
                        projectMemberId,
                        request
                ),
                "Project member role updated."
        );
    }

    @Operation(summary = "프로젝트 탈퇴")
    @DeleteMapping("/me")
    public ApiResponse<Void> leaveProject(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        projectMemberService.leaveProject(memberDetails.getMemberId(), projectId);
        return ApiResponse.success("Project left.");
    }

    @Operation(summary = "프로젝트 멤버 내보내기")
    @DeleteMapping("/{projectMemberId}")
    public ApiResponse<Void> removeMember(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long projectMemberId
    ) {
        projectMemberService.removeMember(
                memberDetails.getMemberId(),
                projectId,
                projectMemberId
        );
        return ApiResponse.success("Project member removed.");
    }
}
