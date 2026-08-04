package com.wrap.domain.project.dto.response;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectStatus;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;


/**
 * 프로젝트 목록 조회에 필요한 요약 정보만 제공하는 응답 DTO입니다.
 * 설명, 목표 등의 상세정보를 제외하고 목록 표시에 필요한 정보만 제공
 */

@Getter
@Builder
public class ProjectSummaryResponse {

    private Long id;
    private String name;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;

    public static ProjectSummaryResponse from(Project project) {
        return ProjectSummaryResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .build();
    }
}
