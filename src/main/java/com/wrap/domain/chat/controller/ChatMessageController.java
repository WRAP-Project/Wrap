package com.wrap.domain.chat.controller;

import com.wrap.domain.chat.dto.request.ChatMessageCreateRequest;
import com.wrap.domain.chat.dto.request.ChatMessageUpdateRequest;
import com.wrap.domain.chat.dto.response.ChatMessageResponse;
import com.wrap.domain.chat.dto.response.ChatMessageListResponse;
import com.wrap.domain.chat.service.ChatMessageService;
import com.wrap.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    private final ChatMessageService chatMessageService;

    @PostMapping("/chat-rooms/{chatRoomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @RequestHeader(MEMBER_ID_HEADER) @Positive Long memberId,
            @PathVariable @Positive Long chatRoomId,
            @Valid @RequestBody ChatMessageCreateRequest request
    ) {
        ChatMessageResponse response = chatMessageService.send(
                memberId,
                chatRoomId,
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "메시지를 전송했습니다."));
    }

    @GetMapping("/chat-rooms/{chatRoomId}/messages")
    public ApiResponse<ChatMessageListResponse> getMessages(
            @RequestHeader(MEMBER_ID_HEADER) @Positive Long memberId,
            @PathVariable @Positive Long chatRoomId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(required = false) @Positive Long afterMessageId,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(
                chatMessageService.getMessages(
                        memberId,
                        chatRoomId,
                        cursor,
                        afterMessageId,
                        size
                ),
                "메시지 목록을 조회했습니다."
        );
    }

    @PatchMapping("/chat-rooms/{chatRoomId}/messages/{messageId}")
    public ApiResponse<ChatMessageResponse> updateMessage(
            @RequestHeader(MEMBER_ID_HEADER) @Positive Long memberId,
            @PathVariable @Positive Long chatRoomId,
            @PathVariable @Positive Long messageId,
            @Valid @RequestBody ChatMessageUpdateRequest request
    ) {
        return ApiResponse.success(
                chatMessageService.update(
                        memberId,
                        chatRoomId,
                        messageId,
                        request
                ),
                "메시지를 수정했습니다."
        );
    }

    @DeleteMapping("/chat-rooms/{chatRoomId}/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @RequestHeader(MEMBER_ID_HEADER) @Positive Long memberId,
            @PathVariable @Positive Long chatRoomId,
            @PathVariable @Positive Long messageId
    ) {
        chatMessageService.delete(memberId, chatRoomId, messageId);
        return ApiResponse.success(null, "메시지를 삭제했습니다.");
    }
}
