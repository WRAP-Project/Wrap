package com.wrap.domain.chat.dto.response;

import com.wrap.domain.chat.entity.ChatRoom;
import com.wrap.domain.chat.enums.ChatRoomStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ChatRoomDetailResponse(
        Long chatRoomId,
        String name,
        ChatRoomStatus status,
        ChatProjectResponse project,
        ChatScheduleResponse schedule,
        ChatMemberResponse creator,
        List<ChatMemberResponse> participants,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {

    public static ChatRoomDetailResponse from(
            ChatRoom chatRoom,
            List<ChatMemberResponse> participants
    ) {
        return new ChatRoomDetailResponse(
                chatRoom.getId(),
                chatRoom.getName(),
                chatRoom.getStatus(),
                ChatProjectResponse.from(chatRoom.getProject()),
                ChatScheduleResponse.from(chatRoom.getSchedule()),
                ChatMemberResponse.from(chatRoom.getCreator()),
                participants,
                chatRoom.getCreatedAt(),
                chatRoom.getClosedAt()
        );
    }
}
