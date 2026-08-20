package com.wrap.domain.invitation.controller;

import com.wrap.domain.invitation.dto.request.InvitationCreateRequest;
import com.wrap.domain.invitation.dto.response.InvitationResponse;
import com.wrap.domain.invitation.dto.response.ReceivedInvitationResponse;
import com.wrap.domain.invitation.service.InvitationService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invitation", description = "프로젝트 초대 관련 API")
@RestController
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @Operation(summary = "받은 프로젝트 초대 목록 조회")
    @GetMapping("/invitations")
    public ApiResponse<List<ReceivedInvitationResponse>> getReceivedInvitations(
            @AuthenticationPrincipal MemberDetails memberDetails
    ) {
        return ApiResponse.success(
                invitationService.getReceivedInvitations(memberDetails.getMemberId()),
                "Received project invitations retrieved."
        );
    }

    @Operation(summary = "프로젝트 팀원 초대 생성")
    @PostMapping("/projects/{projectId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvitationResponse> create(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody InvitationCreateRequest request
    ) {
        return ApiResponse.success(
                invitationService.create(memberDetails.getMemberId(), projectId, request),
                "Project invitation created."
        );
    }
}
