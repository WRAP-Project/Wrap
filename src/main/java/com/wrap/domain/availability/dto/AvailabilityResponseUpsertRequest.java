package com.wrap.domain.availability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AvailabilityResponseUpsertRequest(
        @NotNull(message = "Slots are required.")
        List<@NotNull @Valid AvailabilitySlotRequest> slots
) {
}
