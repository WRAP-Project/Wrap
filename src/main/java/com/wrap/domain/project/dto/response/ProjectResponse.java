package com.wrap.domain.project.dto.response;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 프로젝트 상세 조회에 사용하는 응답 DTO입니다.
 */

@Getter
@Builder
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String goal;
    private String successCriteria;
    private LocalDate startDate;
    private LocalDate endDate;
    private String color;
    private ProjectStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .goal(project.getGoal())
                .successCriteria(project.getSuccessCriteria())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .color(project.getColor())
                .status(project.getStatus())
                .completedAt(project.getCompletedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
