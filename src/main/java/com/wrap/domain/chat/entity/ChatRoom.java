package com.wrap.domain.chat.entity;

import com.wrap.domain.chat.enums.ChatRoomStatus;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.schedule.entity.Schedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_schedule",
                        columnNames = "schedule_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_chat_room_project_status_deleted",
                        columnList = "project_id, status, deleted_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_project_member_id", nullable = false)
    private ProjectMember creator;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status = ChatRoomStatus.OPEN;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_project_member_id")
    private ProjectMember closedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_project_member_id")
    private ProjectMember deletedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ChatRoom(
            Project project,
            Schedule schedule,
            ProjectMember creator,
            String name
    ) {
        this.project = project;
        this.schedule = schedule;
        this.creator = creator;
        this.name = name;
    }

    public static ChatRoom create(
            Project project,
            Schedule schedule,
            ProjectMember creator,
            String name
    ) {
        return new ChatRoom(project, schedule, creator, name);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void close(ProjectMember closedBy, LocalDateTime closedAt) {
        this.status = ChatRoomStatus.CLOSED;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
    }

    public void softDelete(ProjectMember deletedBy, LocalDateTime deletedAt) {
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
    }
}
