package com.wrap.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageUpdateRequest(
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {
}
