package com.wrap.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "프로젝트 부분 수정 요청. 모든 필드는 선택이며 생략하거나 null을 보내면 기존 값을 유지합니다.")
@Getter
@NoArgsConstructor
public class ProjectUpdateRequest {

    @Pattern(
            regexp = "(?s).*\\P{javaWhitespace}.*",
            message = "프로젝트 이름은 공백일 수 없습니다."
    )
    @Size(max = 100, message = "프로젝트 이름은 100자 이하여야 합니다.")
    @Schema(description = "프로젝트 이름. 빈 문자열·공백만 입력은 불가하며 최대 100자입니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String name;

    @Size(max = 2000, message = "프로젝트 설명은 2,000자 이하여야 합니다.")
    @Schema(description = "프로젝트 설명. 최대 2,000자이며 빈 문자열·공백만 보내면 내용을 삭제합니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @Size(max = 2000, message = "프로젝트 목표는 2,000자 이하여야 합니다.")
    @Schema(description = "프로젝트 목표. 최대 2,000자이며 빈 문자열·공백만 보내면 내용을 삭제합니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String goal;

    @Size(max = 2000, message = "성공 기준은 2,000자 이하여야 합니다.")
    @Schema(description = "성공 기준. 최대 2,000자이며 빈 문자열·공백만 보내면 내용을 삭제합니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String successCriteria;

    @Schema(description = "시작일. 기존 값과 합친 결과가 종료일보다 늦으면 안 됩니다. null로 날짜를 삭제할 수 없습니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate startDate;

    @Schema(description = "종료일. 기존 값과 합친 결과가 시작일보다 이르면 안 됩니다. null로 날짜를 삭제할 수 없습니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate endDate;

    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "프로젝트 색상은 #RRGGBB 형식이어야 합니다."
    )
    @Schema(description = "프로젝트 색상. #RRGGBB 형식이며 빈 문자열은 허용하지 않습니다. 생략 또는 null은 기존 값을 유지합니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String color;
}
