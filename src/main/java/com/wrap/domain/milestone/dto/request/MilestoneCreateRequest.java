package com.wrap.domain.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MilestoneCreateRequest(

        @NotBlank(message = "마일스톤 제목은 필수입니다.")
        @Size(max = 100, message = "마일스톤 제목은 100자 이하여야 합니다.")
        String title,

        String description,

        @NotNull(message = "마일스톤 목표일은 필수입니다.")
        LocalDate dueDate
) {

    public MilestoneCreateRequest(String title, LocalDate dueDate) {
        this(title, null, dueDate);
    }
}
