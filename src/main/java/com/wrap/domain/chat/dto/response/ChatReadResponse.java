package com.wrap.domain.chat.dto.response;

import com.wrap.domain.chat.entity.ChatReadState;
import java.time.LocalDateTime;

public record ChatReadResponse(
        Long chatRoomId,
        Long lastReadMessageId,
        LocalDateTime lastReadAt
) {

    public static ChatReadResponse from(ChatReadState readState) {
        return new ChatReadResponse(
                readState.getChatRoom().getId(),
                readState.getLastReadMessage().getId(),
                readState.getLastReadAt()
        );
    }
}
