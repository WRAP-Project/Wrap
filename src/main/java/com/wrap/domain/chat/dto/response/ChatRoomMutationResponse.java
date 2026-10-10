package com.wrap.domain.chat.dto.response;

import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import java.time.LocalDateTime;

public record ChatRoomMutationResponse(
        Long chatRoomId,
        String name,
        ChatRoomStatus status,
        LocalDateTime closedAt
) {

    public static ChatRoomMutationResponse from(ChatRoom chatRoom) {
        return new ChatRoomMutationResponse(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getStatus(),
                chatRoom.getClosedAt()
        );
    }
}
