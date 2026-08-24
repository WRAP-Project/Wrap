package com.wrap.domain.availability.dto;

import com.wrap.domain.availability.entity.AvailabilityRequest;
import com.wrap.domain.availability.enums.AvailabilityRequestStatus;
import java.time.LocalDate;

public record AvailabilityRequestDetailResponse(
        Long availabilityRequestId,
        Long projectId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        int slotUnitMinutes,
        AvailabilityRequestStatus status,
        int totalMemberCount,
        long submittedMemberCount,
        Long confirmedScheduleId
) {

    public static AvailabilityRequestDetailResponse of(
            AvailabilityRequest request,
            int totalMemberCount,
            long submittedMemberCount
    ) {
        return new AvailabilityRequestDetailResponse(
                request.getId(),
                request.getProject().getId(),
                request.getTitle(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate(),
                request.getSlotUnitMinutes(),
                request.getStatus(),
                totalMemberCount,
                submittedMemberCount,
                request.getConfirmedSchedule() == null ? null : request.getConfirmedSchedule().getId()
        );
    }
}
