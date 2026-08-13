package com.wrap.domain.availability.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AvailabilitySlotRequest(
        @NotNull(message = "Slot start time is required.")
        LocalDateTime startAt,

        @NotNull(message = "Slot end time is required.")
        LocalDateTime endAt
) {
}
