package com.wrap.domain.availability.entity;

import com.wrap.domain.projectmember.entity.ProjectMember;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "availability_response",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_availability_response_request_member",
                        columnNames = {"availability_request_id", "project_member_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "availability_request_id", nullable = false)
    private AvailabilityRequest availabilityRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_member_id", nullable = false)
    private ProjectMember projectMember;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "availabilityResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvailabilitySlot> slots = new ArrayList<>();

    public static AvailabilityResponse create(
            AvailabilityRequest availabilityRequest,
            ProjectMember projectMember,
            LocalDateTime submittedAt
    ) {
        AvailabilityResponse response = new AvailabilityResponse();
        response.availabilityRequest = availabilityRequest;
        response.projectMember = projectMember;
        response.submittedAt = submittedAt;
        return response;
    }

    public void replaceSlots(List<AvailabilitySlot> newSlots, LocalDateTime submittedAt) {
        slots.clear();
        newSlots.forEach(slot -> slot.assignResponse(this));
        slots.addAll(newSlots);
        this.submittedAt = submittedAt;
    }
}
