package com.wrap.domain.chat.controller;

import com.wrap.domain.chat.dto.request.ChatRoomCreateRequest;
import com.wrap.domain.chat.dto.request.ChatRoomUpdateRequest;
import com.wrap.domain.chat.dto.response.ChatRoomDetailResponse;
import com.wrap.domain.chat.dto.response.ChatRoomListResponse;
import com.wrap.domain.chat.dto.response.ChatRoomMutationResponse;
import com.wrap.domain.chat.dto.response.ChatMemberResponse;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import com.wrap.domain.chat.service.ChatRoomService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/projects/{projectId}/chat-rooms")
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> createChatRoom(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody ChatRoomCreateRequest request
    ) {
        ChatRoomDetailResponse response = chatRoomService.create(
                memberDetails.getMemberId(),
                projectId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "채팅방을 생성했습니다."));
    }

    @GetMapping("/chat-rooms")
    public ApiResponse<ChatRoomListResponse> getChatRooms(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestParam(defaultValue = "OPEN") ChatRoomStatus status,
            @RequestParam(required = false) @Positive Long projectId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                chatRoomService.getChatRooms(
                        memberDetails.getMemberId(),
                        status,
                        projectId,
                        query,
                        cursor,
                        size
                ),
                "채팅방 목록을 조회했습니다."
        );
    }

    @GetMapping("/chat-rooms/{chatRoomId}")
    public ApiResponse<ChatRoomDetailResponse> getChatRoom(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long chatRoomId
    ) {
        return ApiResponse.success(
                chatRoomService.getChatRoom(memberDetails.getMemberId(), chatRoomId),
                "채팅방을 조회했습니다."
        );
    }

    @GetMapping("/chat-rooms/{chatRoomId}/members")
    public ApiResponse<List<ChatMemberResponse>> getChatRoomMembers(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long chatRoomId
    ) {
        return ApiResponse.success(
                chatRoomService.getMembers(memberDetails.getMemberId(), chatRoomId),
                "채팅방 참여자 목록을 조회했습니다."
        );
    }

    @PatchMapping("/chat-rooms/{chatRoomId}")
    public ApiResponse<ChatRoomMutationResponse> updateChatRoomName(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long chatRoomId,
            @Valid @RequestBody ChatRoomUpdateRequest request
    ) {
        return ApiResponse.success(
                chatRoomService.updateName(memberDetails.getMemberId(), chatRoomId, request),
                "채팅방 이름을 수정했습니다."
        );
    }

    @PatchMapping("/chat-rooms/{chatRoomId}/close")
    public ApiResponse<ChatRoomMutationResponse> closeChatRoom(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long chatRoomId
    ) {
        return ApiResponse.success(
                chatRoomService.close(memberDetails.getMemberId(), chatRoomId),
                "채팅방을 종료했습니다."
        );
    }

    @DeleteMapping("/chat-rooms/{chatRoomId}")
    public ApiResponse<Void> deleteChatRoom(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable @Positive Long chatRoomId
    ) {
        chatRoomService.delete(memberDetails.getMemberId(), chatRoomId);
        return ApiResponse.success("채팅방을 삭제했습니다.");
    }
}
