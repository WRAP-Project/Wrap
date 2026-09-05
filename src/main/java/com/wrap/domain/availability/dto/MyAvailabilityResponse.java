package com.wrap.domain.availability.dto;

import com.wrap.domain.availability.entity.AvailabilityRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MyAvailabilityResponse(
        Long availabilityRequestId,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        int slotUnitMinutes,
        List<BusySlotResponse> existingBusySlots,
        List<AvailabilitySlotResponse> selectedAvailableSlots
) {

    public static MyAvailabilityResponse of(
            AvailabilityRequest request,
            List<BusySlotResponse> existingBusySlots,
            List<AvailabilitySlotResponse> selectedAvailableSlots
    ) {
        return new MyAvailabilityResponse(
                request.getId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getSlotUnitMinutes(),
                existingBusySlots,
                selectedAvailableSlots
        );
    }
}
