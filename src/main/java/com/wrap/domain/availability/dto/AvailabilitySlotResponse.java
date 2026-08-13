package com.wrap.domain.availability.dto;

import com.wrap.domain.availability.entity.AvailabilitySlot;
import java.time.LocalDateTime;

public record AvailabilitySlotResponse(
        LocalDateTime startAt,
        LocalDateTime endAt
) {

    public static AvailabilitySlotResponse from(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(slot.getStartAt(), slot.getEndAt());
    }
}
