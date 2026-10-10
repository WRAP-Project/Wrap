package com.wrap.domain.chat.dto.response;

import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;

public record ChatScheduleResponse(
        Long scheduleId,
        LocalDateTime startAt,
        LocalDateTime endAt
) {

    public static ChatScheduleResponse from(Schedule schedule) {
        if (schedule == null) {
            return null;
        }
        return new ChatScheduleResponse(
                schedule.getId(),
                schedule.getStartAt(),
                schedule.getEndAt()
        );
    }
}
