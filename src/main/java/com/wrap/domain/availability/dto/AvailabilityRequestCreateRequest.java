package com.wrap.domain.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityRequestCreateRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 100, message = "Title must be 100 characters or less.")
        String title,

        String description,

        @NotNull(message = "Start date is required.")
        LocalDate startDate,

        @NotNull(message = "End date is required.")
        LocalDate endDate,

        LocalTime startTime,

        LocalTime endTime,

        @NotNull(message = "Slot unit minutes is required.")
        Integer slotUnitMinutes
) {

    public AvailabilityRequestCreateRequest(
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            Integer slotUnitMinutes
    ) {
        this(title, description, startDate, endDate, null, null, slotUnitMinutes);
    }
}
