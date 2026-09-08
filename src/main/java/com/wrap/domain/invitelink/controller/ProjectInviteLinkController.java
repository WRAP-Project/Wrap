package com.wrap.domain.invitelink.controller;

import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkInfoResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteJoinResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkSummaryResponse;
import com.wrap.domain.invitelink.service.ProjectInviteLinkService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Project Invite Link",
        description = "프로젝트 초대 링크 생성, 조회, 비활성화 및 참여 API"
)
@RestController
@RequiredArgsConstructor
public class ProjectInviteLinkController {

    private final ProjectInviteLinkService inviteLinkService;

    @Operation(
            summary = "프로젝트 초대 링크 생성",
            description = """
                    프로젝트 OWNER가 공유 가능한 초대 링크를 생성합니다.
                    프로젝트당 하나의 활성 링크만 존재할 수 있으며,
                    원본 초대 URL은 생성 성공 응답에서만 반환됩니다.
                    """
    )
    @SecurityRequirement(name = "sessionAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "초대 링크 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인이 필요함"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한이 없거나 OWNER가 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "프로젝트를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "활성 링크가 이미 존재하거나 프로젝트가 완료됨"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500", description = "고유 토큰 생성 실패"
            )
    })
    @PostMapping("/projects/{projectId}/invite-links")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectInviteLinkResponse> create(
            @Parameter(hidden = true)
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                inviteLinkService.create(memberDetails.getMemberId(), projectId),
                "Project invite link created."
        );
    }

    @Operation(
            summary = "프로젝트 초대 링크 목록 조회",
            description = """
                    프로젝트에서 생성된 초대 링크 이력을 최신순으로 조회합니다.
                    보안을 위해 원본 토큰과 전체 초대 URL은 반환하지 않습니다.
                    """
    )
    @SecurityRequirement(name = "sessionAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "초대 링크 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인이 필요함"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한이 없거나 OWNER가 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "프로젝트를 찾을 수 없음"
            )
    })
    @GetMapping("/projects/{projectId}/invite-links")
    public ApiResponse<List<ProjectInviteLinkSummaryResponse>> getInviteLinks(
            @Parameter(hidden = true)
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable Long projectId
    ) {
        return ApiResponse.success(
                inviteLinkService.getInviteLinks(memberDetails.getMemberId(), projectId),
                "Project invite links retrieved."
        );
    }

    @Operation(
            summary = "프로젝트 초대 링크 비활성화",
            description = """
                    활성 초대 링크를 비활성화합니다.
                    링크 데이터는 삭제하지 않으며 비활성화 상태와 시각을 기록합니다.
                    비활성화된 링크로는 프로젝트 정보 조회와 참여가 불가능합니다.
                    """
    )
    @SecurityRequirement(name = "sessionAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "초대 링크 비활성화 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인이 필요함"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한이 없거나 OWNER가 아님"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 또는 초대 링크를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "프로젝트가 완료되었거나 링크가 이미 비활성화됨"
            )
    })
    @DeleteMapping("/projects/{projectId}/invite-links/{inviteLinkId}")
    public ApiResponse<Void> revoke(
            @Parameter(hidden = true)
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Parameter(description = "프로젝트 ID", example = "10")
            @PathVariable Long projectId,
            @Parameter(description = "비활성화할 초대 링크 ID", example = "1")
            @PathVariable Long inviteLinkId
    ) {
        inviteLinkService.revoke(
                memberDetails.getMemberId(),
                projectId,
                inviteLinkId
        );
        return ApiResponse.success("Project invite link revoked.");
    }

    @Operation(
            summary = "초대 링크 프로젝트 정보 조회",
            description = """
                    로그인하지 않은 사용자도 호출할 수 있는 공개 API입니다.
                    유효한 링크의 프로젝트 이름, 색상과 초대한 사용자의 닉네임을 반환합니다.
                    회원 이메일과 토큰 해시 등 민감한 정보는 반환하지 않습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "초대 프로젝트 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "링크가 없거나 비활성화되었거나 프로젝트가 삭제됨"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "프로젝트가 완료됨"
            )
    })
    @GetMapping("/invite-links/{token}")
    public ApiResponse<ProjectInviteLinkInfoResponse> getInviteLinkInfo(
            @Parameter(
                    description = "초대 URL에 포함된 원본 토큰",
                    example = "xYz123_exampleToken"
            )
            @PathVariable String token
    ) {
        return ApiResponse.success(
                inviteLinkService.getInviteLinkInfo(token),
                "Project invite link information retrieved."
        );
    }

    @Operation(
            summary = "초대 링크를 통한 프로젝트 참여",
            description = """
                    로그인한 사용자가 유효한 초대 링크를 통해 프로젝트에 참여합니다.
                    신규 참여자와 재참여자의 역할은 MEMBER로 설정됩니다.
                    이미 참여 중인 사용자는 중복으로 참여할 수 없습니다.
                    """
    )
    @SecurityRequirement(name = "sessionAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "프로젝트 참여 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "로그인이 필요함"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "링크 또는 활성 회원을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "프로젝트가 완료되었거나 이미 참여 중임"
            )
    })
    @PostMapping("/invite-links/{token}/join")
    public ApiResponse<ProjectInviteJoinResponse> join(
            @Parameter(hidden = true)
            @AuthenticationPrincipal MemberDetails memberDetails,
            @Parameter(
                    description = "초대 URL에 포함된 원본 토큰",
                    example = "xYz123_exampleToken"
            )
            @PathVariable String token
    ) {
        return ApiResponse.success(
                inviteLinkService.join(memberDetails.getMemberId(), token),
                "Joined project through invite link."
        );
    }
}
