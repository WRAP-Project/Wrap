package com.wrap.domain.chat.dto.response;

import java.util.List;

public record ChatMessageListResponse(
        List<ChatMessageResponse> content,
        Long nextCursor,
        boolean hasNext
) {
}
