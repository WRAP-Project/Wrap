package com.wrap.domain.chat.entity;

import com.wrap.domain.projectmember.entity.ProjectMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "chat_read_state",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_read_state_room_project_member",
                        columnNames = {"chat_room_id", "project_member_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_member_id", nullable = false)
    private ProjectMember projectMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private ChatMessage lastReadMessage;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ChatReadState(ChatRoom chatRoom, ProjectMember projectMember) {
        this.chatRoom = chatRoom;
        this.projectMember = projectMember;
    }

    public static ChatReadState create(ChatRoom chatRoom, ProjectMember projectMember) {
        return new ChatReadState(chatRoom, projectMember);
    }

    public void updateLastRead(ChatMessage lastReadMessage, LocalDateTime lastReadAt) {
        this.lastReadMessage = lastReadMessage;
        this.lastReadAt = lastReadAt;
    }
}
