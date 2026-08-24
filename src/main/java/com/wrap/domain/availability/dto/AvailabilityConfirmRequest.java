package com.wrap.domain.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record AvailabilityConfirmRequest(
        @NotBlank(message = "Title is required.")
        @Size(max = 100, message = "Title must be 100 characters or less.")
        String title,

        String description,

        @NotNull(message = "Start time is required.")
        LocalDateTime startAt,

        @NotNull(message = "End time is required.")
        LocalDateTime endAt
) {
}
