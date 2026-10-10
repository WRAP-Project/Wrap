package com.wrap.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ChatRoomCreateRequest(
        @NotBlank(message = "채팅방 이름은 필수입니다.")
        @Size(max = 100, message = "채팅방 이름은 100자 이하여야 합니다.")
        String name,

        @NotNull(message = "참여자 목록은 필수입니다.")
        List<@NotNull @Positive Long> participantProjectMemberIds,

        @Positive(message = "일정 ID는 양수여야 합니다.")
        Long scheduleId
) {
}
