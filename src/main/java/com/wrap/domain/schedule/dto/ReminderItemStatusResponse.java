package com.wrap.domain.schedule.dto;

import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistSourceType;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistStatus;

public record ReminderItemStatusResponse(
        ReminderChecklistSourceType sourceType,
        Long sourceId,
        ReminderChecklistStatus status,
        String statusLabel
) {

    public static ReminderItemStatusResponse of(
            ReminderChecklistSourceType sourceType,
            Long sourceId,
            ReminderChecklistStatus status
    ) {
        return new ReminderItemStatusResponse(
                sourceType,
                sourceId,
                status,
                status.label()
        );
    }
}
