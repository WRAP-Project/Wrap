package com.wrap.domain.availability.dto;

import java.time.LocalDateTime;

public record RecommendedSlotResponse(
        LocalDateTime startAt,
        LocalDateTime endAt,
        int availableCount,
        int totalMemberCount
) {
}
