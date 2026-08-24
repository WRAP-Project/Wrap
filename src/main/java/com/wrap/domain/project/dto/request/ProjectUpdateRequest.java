package com.wrap.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectUpdateRequest {

    @NotBlank(message = "프로젝트 이름은 필수입니다.")
    @Size(max = 100, message = "프로젝트 이름은 100자 이하여야 합니다.")
    private String name;

    @Size(max = 2000, message = "프로젝트 설명은 2,000자 이하여야 합니다.")
    private String description;

    @Size(max = 2000, message = "프로젝트 목표는 2,000자 이하여야 합니다.")
    private String goal;

    @Size(max = 2000, message = "성공 기준은 2,000자 이하여야 합니다.")
    private String successCriteria;

    private LocalDate startDate;

    private LocalDate endDate;

    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "프로젝트 색상은 #RRGGBB 형식이어야 합니다."
    )
    private String color;
}
