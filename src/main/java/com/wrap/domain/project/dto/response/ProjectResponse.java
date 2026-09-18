package com.wrap.domain.project.dto.response;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 프로젝트 상세 조회 및 생성·수정·완료·재개에 사용하는 응답 DTO입니다.
 */

@Schema(description = "프로젝트 상세 조회 및 생성·수정·완료·재개 응답. 현재 요청자의 프로젝트 관리 권한을 포함합니다.")
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

    @Schema(
            description = "현재 요청자의 프로젝트 관리 권한. OWNER는 관리자, MEMBER는 일반 팀원이며 업무 역할(workRole)과는 별개입니다.",
            example = "OWNER",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ProjectMemberRole myRole;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectResponse from(Project project, ProjectMemberRole myRole) {
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
                .myRole(myRole)
                .completedAt(project.getCompletedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
