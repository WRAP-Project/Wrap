package com.wrap.domain.availability.dto;

import java.util.List;

public record AvailabilityResponsesResponse(
        Long availabilityRequestId,
        List<MemberAvailabilityResponse> members
) {
}
