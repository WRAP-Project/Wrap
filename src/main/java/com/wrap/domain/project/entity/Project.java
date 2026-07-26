package com.wrap.domain.project.entity;

import com.wrap.domain.project.enums.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "project")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Column(name = "success_criteria", columnDefinition = "TEXT")
    private String successCriteria;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Project create(
            String name,
            String description,
            String goal,
            String successCriteria,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Project project = new Project();
        project.name = normalizeRequiredName(name);
        project.description = normalizeOptional(description, "프로젝트 설명");
        project.goal = normalizeOptional(goal, "프로젝트 목표");
        project.successCriteria = normalizeOptional(successCriteria, "성공 기준");
        project.startDate = startDate;
        project.endDate = endDate;
        project.status = ProjectStatus.IN_PROGRESS;
        validateDateRange(startDate, endDate);
        return project;
    }

    public void update(
            String name,
            String description,
            String goal,
            String successCriteria,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String normalizedName = normalizeRequiredName(name);
        String normalizedDescription = normalizeOptional(description, "프로젝트 설명");
        String normalizedGoal = normalizeOptional(goal, "프로젝트 목표");
        String normalizedSuccessCriteria = normalizeOptional(successCriteria, "성공 기준");
        validateDateRange(startDate, endDate);

        this.name = normalizedName;
        this.description = normalizedDescription;
        this.goal = normalizedGoal;
        this.successCriteria = normalizedSuccessCriteria;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void complete(LocalDateTime completedAt) {
        if (status != ProjectStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 프로젝트만 완료할 수 있습니다.");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("프로젝트 완료 시각은 필수입니다.");
        }

        this.status = ProjectStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void reopen() {
        if (status != ProjectStatus.COMPLETED) {
            throw new IllegalStateException("완료된 프로젝트만 재진행할 수 있습니다.");
        }

        this.status = ProjectStatus.IN_PROGRESS;
        this.completedAt = null;
    }

    public void softDelete(LocalDateTime deletedAt) {
        if (isDeleted()) {
            throw new IllegalStateException("이미 삭제된 프로젝트입니다.");
        }
        if (deletedAt == null) {
            throw new IllegalArgumentException("프로젝트 삭제 시각은 필수입니다.");
        }

        this.deletedAt = deletedAt;
    }

    public boolean isCompleted() {
        return status == ProjectStatus.COMPLETED;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("프로젝트 시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    private static String normalizeRequiredName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("프로젝트 이름은 필수입니다.");
        }

        String normalized = value.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("프로젝트 이름은 100자를 초과할 수 없습니다.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > 2000) {
            throw new IllegalArgumentException(fieldName + "은 2,000자를 초과할 수 없습니다.");
        }
        return normalized;
    }
}
