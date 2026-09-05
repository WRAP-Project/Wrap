package com.wrap.domain.availability.dto;

import com.wrap.domain.availability.entity.AvailabilityRequest;
import com.wrap.domain.availability.enums.AvailabilityRequestStatus;
import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityRequestSummaryResponse(
        Long availabilityRequestId,
        Long projectId,
        Long creatorProjectMemberId,
        String creatorNickname,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        int slotUnitMinutes,
        AvailabilityRequestStatus status
) {

    public static AvailabilityRequestSummaryResponse from(AvailabilityRequest request) {
        return new AvailabilityRequestSummaryResponse(
                request.getId(),
                request.getProject().getId(),
                request.getCreator().getId(),
                request.getCreator().getMember().getNickname(),
                request.getTitle(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getSlotUnitMinutes(),
                request.getStatus()
        );
    }
}
