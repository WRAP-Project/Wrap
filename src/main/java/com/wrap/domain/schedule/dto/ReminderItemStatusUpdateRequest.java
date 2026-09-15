package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistStatus;
import jakarta.validation.constraints.NotNull;

public record ReminderItemStatusUpdateRequest(
        @NotNull ReminderChecklistStatus status
) {
}
