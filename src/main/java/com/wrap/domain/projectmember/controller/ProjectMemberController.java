package com.wrap.domain.projectmember.controller;

import com.wrap.domain.projectmember.dto.request.ProjectMemberRoleUpdateRequest;
import com.wrap.domain.projectmember.dto.response.ProjectMemberResponse;
import com.wrap.domain.projectmember.service.ProjectMemberService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "프로젝트 멤버 역할 변경",
            description = """
                    진행 중인 프로젝트의 참여 중인(JOINED) OWNER만 관리 권한을 변경할 수 있습니다.
                    참여 중인 마지막 OWNER를 MEMBER로 변경할 수 없습니다.
                    같은 프로젝트의 탈퇴·권한 변경·내보내기가 동시에 요청되어도 OWNER는 최소 한 명 유지됩니다.
                    처리 시점의 최신 참여 상태와 권한을 기준으로 검사하므로 대기 중 권한을 잃으면 요청이 거부됩니다.
                    업무 역할(workRole)이 아닌 OWNER/MEMBER 관리 권한을 변경합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "프로젝트 멤버 권한 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "입력값 검증 실패(VALIDATION_FAILED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인 필요(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "참여 권한 없음(PROJECT_ACCESS_DENIED) 또는 OWNER 권한 필요(PROJECT_OWNER_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 없음(PROJECT_NOT_FOUND) 또는 참여 중인 대상 멤버 없음(PROJECT_MEMBER_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "마지막 OWNER의 권한 하향 불가(LAST_PROJECT_OWNER) 또는 완료 프로젝트(PROJECT_ALREADY_COMPLETED)")
    })
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

    @Operation(
            summary = "프로젝트 탈퇴",
            description = """
                    진행 중인 프로젝트에서 현재 로그인한 참여자(JOINED)가 탈퇴합니다.
                    참여 중인 마지막 OWNER는 탈퇴할 수 없습니다.
                    다른 OWNER의 탈퇴·권한 변경과 동시에 요청되어도 OWNER는 최소 한 명 유지됩니다.
                    처리 시점에 이미 탈퇴한 요청자는 PROJECT_ACCESS_DENIED로 거부됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "프로젝트 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인 필요(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "참여 권한 없음(PROJECT_ACCESS_DENIED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "프로젝트 없음(PROJECT_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "마지막 OWNER 탈퇴 불가(LAST_PROJECT_OWNER) 또는 완료 프로젝트(PROJECT_ALREADY_COMPLETED)")
    })
    @DeleteMapping("/me")
    public ApiResponse<Void> leaveProject(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId
    ) {
        projectMemberService.leaveProject(memberDetails.getMemberId(), projectId);
        return ApiResponse.success("Project left.");
    }

    @Operation(
            summary = "프로젝트 멤버 내보내기",
            description = """
                    진행 중인 프로젝트의 참여 중인(JOINED) OWNER가 일반 MEMBER를 내보냅니다.
                    OWNER는 내보낼 수 없으며, 동시 권한 변경으로 대상이 OWNER가 된 경우에도 요청이 거부됩니다.
                    처리 시점의 요청자 권한과 대상의 참여 상태·권한을 기준으로 검사합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "프로젝트 멤버 내보내기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인 필요(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "참여 권한 없음(PROJECT_ACCESS_DENIED) 또는 OWNER 권한 필요(PROJECT_OWNER_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 없음(PROJECT_NOT_FOUND) 또는 참여 중인 대상 멤버 없음(PROJECT_MEMBER_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "OWNER 내보내기 불가(PROJECT_OWNER_CANNOT_BE_REMOVED) 또는 완료 프로젝트(PROJECT_ALREADY_COMPLETED)")
    })
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
