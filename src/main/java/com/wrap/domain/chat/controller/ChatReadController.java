package com.wrap.domain.chat.controller;

import com.wrap.domain.chat.dto.request.ChatReadRequest;
import com.wrap.domain.chat.dto.response.ChatReadResponse;
import com.wrap.domain.chat.service.ChatReadService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class ChatReadController {

    private final ChatReadService chatReadService;

    @PutMapping("/chat-rooms/{chatRoomId}/read")
    public ApiResponse<ChatReadResponse> updateLastRead(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long chatRoomId,
            @Valid @RequestBody ChatReadRequest request
    ) {
        return ApiResponse.success(
                chatReadService.updateLastRead(memberDetails.getMemberId(), chatRoomId, request),
                "마지막 읽음 위치를 갱신했습니다."
        );
    }
}
