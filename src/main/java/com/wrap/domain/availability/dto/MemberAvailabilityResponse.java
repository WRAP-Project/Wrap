package com.wrap.domain.availability.dto;

import java.util.List;

public record MemberAvailabilityResponse(
        Long projectMemberId,
        String nickname,
        boolean submitted,
        List<AvailabilitySlotResponse> slots
) {
}
