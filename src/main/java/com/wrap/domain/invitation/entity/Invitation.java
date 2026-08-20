package com.wrap.domain.invitation.entity;

import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(name = "invitation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private Member inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private Member invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Invitation create(
            Project project,
            Member inviter,
            Member invitee,
            ProjectMemberRole role
    ) {
        Invitation invitation = new Invitation();
        invitation.project = requireProject(project);
        invitation.inviter = requireMember(inviter, "초대한 회원은 필수입니다.");
        invitation.invitee = requireMember(invitee, "초대 대상 회원은 필수입니다.");
        invitation.role = requireRole(role);
        invitation.status = InvitationStatus.INVITED;
        return invitation;
    }

    private static Project requireProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("프로젝트는 필수입니다.");
        }
        return project;
    }

    private static Member requireMember(Member member, String message) {
        if (member == null) {
            throw new IllegalArgumentException(message);
        }
        return member;
    }

    private static ProjectMemberRole requireRole(ProjectMemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("초대 역할은 필수입니다.");
        }
        return role;
    }
}
