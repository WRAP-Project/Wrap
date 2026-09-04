package com.wrap.domain.availability.entity;

import com.wrap.domain.availability.enums.AvailabilityRequestStatus;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(name = "availability_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityRequest {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.MIN;
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(23, 59, 59);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private ProjectMember creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false, columnDefinition = "time default '00:00:00'")
    private LocalTime startTime = DEFAULT_START_TIME;

    @Column(name = "end_time", nullable = false, columnDefinition = "time default '23:59:59'")
    private LocalTime endTime = DEFAULT_END_TIME;

    @Column(name = "slot_unit_minutes", nullable = false)
    private int slotUnitMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityRequestStatus status = AvailabilityRequestStatus.OPEN;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_schedule_id")
    private Schedule confirmedSchedule;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AvailabilityRequest create(
            Project project,
            ProjectMember creator,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            int slotUnitMinutes
    ) {
        return create(project, creator, title, description, startDate, endDate, DEFAULT_START_TIME, DEFAULT_END_TIME,
                slotUnitMinutes);
    }

    public static AvailabilityRequest create(
            Project project,
            ProjectMember creator,
            String title,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            int slotUnitMinutes
    ) {
        AvailabilityRequest request = new AvailabilityRequest();
        request.project = project;
        request.creator = creator;
        request.title = title;
        request.description = description;
        request.startDate = startDate;
        request.endDate = endDate;
        request.startTime = startTime == null ? DEFAULT_START_TIME : startTime;
        request.endTime = endTime == null ? DEFAULT_END_TIME : endTime;
        request.slotUnitMinutes = slotUnitMinutes;
        request.status = AvailabilityRequestStatus.OPEN;
        return request;
    }

    public void confirm(Schedule schedule) {
        if (status == AvailabilityRequestStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.AVAILABILITY_REQUEST_ALREADY_CONFIRMED);
        }
        if (status == AvailabilityRequestStatus.CANCELED) {
            throw new CustomException(ErrorCode.AVAILABILITY_REQUEST_CANCELED);
        }
        this.status = AvailabilityRequestStatus.CONFIRMED;
        this.confirmedSchedule = schedule;
    }

    public boolean isOpen() {
        return status == AvailabilityRequestStatus.OPEN;
    }
}
