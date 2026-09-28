package com.wrap.domain.milestone.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MilestoneUpdateRequest(

        @Size(max = 100, message = "마일스톤 제목은 100자 이하여야 합니다.")
        String title,

        String description,

        LocalDate dueDate
) {
}
