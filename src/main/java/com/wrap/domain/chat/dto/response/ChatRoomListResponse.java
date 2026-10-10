package com.wrap.domain.chat.dto.response;

import java.util.List;

public record ChatRoomListResponse(
        List<ChatRoomSummaryResponse> content,
        Long nextCursor,
        boolean hasNext
) {
}
