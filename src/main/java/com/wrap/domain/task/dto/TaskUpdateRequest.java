package com.wrap.domain.task.dto;

import com.wrap.domain.task.enums.TaskPriority;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskUpdateRequest(

        @Size(max = 255, message = "할 일 제목은 255자 이하여야 합니다.")
        String title,

        Long milestoneId,

        String description,

        LocalDate dueDate,

        Long assigneeId,

        TaskPriority priority,

        Boolean deliverable
) {
}
