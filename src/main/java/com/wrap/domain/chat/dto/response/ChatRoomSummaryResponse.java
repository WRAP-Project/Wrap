package com.wrap.domain.chat.dto.response;

import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import java.time.LocalDateTime;

public record ChatRoomSummaryResponse(
        Long chatRoomId,
        String name,
        ChatRoomStatus status,
        ChatProjectResponse project,
        ChatScheduleResponse schedule,
        long participantCount,
        long unreadCount,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {

    public static ChatRoomSummaryResponse from(
            ChatRoom chatRoom,
            long participantCount,
            long unreadCount
    ) {
        return new ChatRoomSummaryResponse(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getStatus(),
                ChatProjectResponse.from(chatRoom.getProject()),
                ChatScheduleResponse.from(chatRoom.getSchedule()),
                participantCount,
                unreadCount,
                chatRoom.getCreatedAt(),
                chatRoom.getClosedAt()
        );
    }
}
