package com.wrap.domain.availability.dto;

import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;

public record BusySlotResponse(
        LocalDateTime startAt,
        LocalDateTime endAt,
        String source
) {

    public static BusySlotResponse fromSchedule(Schedule schedule) {
        return new BusySlotResponse(schedule.getStartAt(), schedule.getEndAt(), "SCHEDULE");
    }
}
