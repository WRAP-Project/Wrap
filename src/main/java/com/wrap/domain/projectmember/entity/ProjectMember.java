package com.wrap.domain.projectmember.entity;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Entity
@Table(
        name = "project_member",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_member_member_project",
                        columnNames = {"member_id", "project_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectMemberStatus status;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ProjectMember createOwner(
            Member member,
            Project project,
            LocalDateTime joinedAt
    ) {
        return createJoined(member, project, ProjectMemberRole.OWNER, joinedAt);
    }

    public static ProjectMember join(
            Member member,
            Project project,
            ProjectMemberRole role,
            LocalDateTime joinedAt
    ) {
        return createJoined(member, project, role, joinedAt);
    }

    public void rejoin(ProjectMemberRole newRole, LocalDateTime joinedAt) {
        if (status != ProjectMemberStatus.LEFT) {
            throw new IllegalStateException("나간 프로젝트 멤버만 다시 참여할 수 있습니다.");
        }

        this.role = requireRole(newRole);
        this.status = ProjectMemberStatus.JOINED;
        this.joinedAt = requireJoinedAt(joinedAt);
    }

    public void changeRole(ProjectMemberRole newRole) {
        requireJoinedMember();
        this.role = requireRole(newRole);
    }

    public void leave() {
        requireJoinedMember();
        this.status = ProjectMemberStatus.LEFT;
    }

    public boolean isOwner() {
        return role == ProjectMemberRole.OWNER;
    }

    public boolean isJoined() {
        return status == ProjectMemberStatus.JOINED;
    }

    public boolean isJoinedOwner() {
        return isJoined() && isOwner();
    }

    private static ProjectMember createJoined(
            Member member,
            Project project,
            ProjectMemberRole role,
            LocalDateTime joinedAt
    ) {
        ProjectMember projectMember = new ProjectMember();
        projectMember.member = requireMember(member);
        projectMember.project = requireProject(project);
        projectMember.role = requireRole(role);
        projectMember.status = ProjectMemberStatus.JOINED;
        projectMember.joinedAt = requireJoinedAt(joinedAt);
        return projectMember;
    }

    private void requireJoinedMember() {
        if (!isJoined()) {
            throw new IllegalStateException("참여 중인 프로젝트 멤버만 변경할 수 있습니다.");
        }
    }

    private static Member requireMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("회원은 필수입니다.");
        }
        return member;
    }

    private static Project requireProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("프로젝트는 필수입니다.");
        }
        return project;
    }

    private static ProjectMemberRole requireRole(ProjectMemberRole role) {
        if (role == null) {
            throw new IllegalArgumentException("프로젝트 역할은 필수입니다.");
        }
        return role;
    }

    private static LocalDateTime requireJoinedAt(LocalDateTime joinedAt) {
        if (joinedAt == null) {
            throw new IllegalArgumentException("프로젝트 참여 시각은 필수입니다.");
        }
        return joinedAt;
    }
}
