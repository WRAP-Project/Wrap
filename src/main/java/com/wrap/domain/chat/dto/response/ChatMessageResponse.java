package com.wrap.domain.chat.dto.response;

import com.wrap.domain.chat.entity.ChatMessage;
import com.wrap.domain.chat.enums.ChatMessageType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long chatRoomId,
        ChatMessageType type,
        ChatMemberResponse sender,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted,
        boolean mine
) {

    public static ChatMessageResponse from(ChatMessage message, Long currentProjectMemberId) {
        boolean deleted = message.getDeletedAt() != null;
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getType(),
                ChatMemberResponse.from(message.getSender()),
                deleted ? null : message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                deleted,
                message.getSender().getId().equals(currentProjectMemberId)
        );
    }
}
