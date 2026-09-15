package com.wrap.domain.task.dto;

import com.wrap.domain.task.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
        @NotNull TaskStatus status
) {
}
