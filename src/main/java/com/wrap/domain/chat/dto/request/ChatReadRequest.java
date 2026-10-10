package com.wrap.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatReadRequest(
        @NotNull(message = "마지막으로 읽은 메시지 ID는 필수입니다.")
        @Positive(message = "메시지 ID는 양수여야 합니다.")
        Long lastReadMessageId
) {
}
