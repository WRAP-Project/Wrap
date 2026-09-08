package com.wrap.domain.invitelink.entity;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
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

@Getter
@Entity
@Table(
        name = "project_invite_link",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_invite_link_token_hash",
                        columnNames = "token_hash"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectInviteLink {

    private static final int SHA_256_HEX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Member createdBy;

    @Column(name = "token_hash", nullable = false, length = SHA_256_HEX_LENGTH)
    private String tokenHash;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static ProjectInviteLink create(
            Project project,
            Member createdBy,
            String tokenHash
    ) {
        ProjectInviteLink inviteLink = new ProjectInviteLink();
        inviteLink.project = requireProject(project);
        inviteLink.createdBy = requireCreatedBy(createdBy);
        inviteLink.tokenHash = requireTokenHash(tokenHash);
        inviteLink.active = true;
        return inviteLink;
    }

    public void revoke(LocalDateTime revokedAt) {
        if (!active) {
            throw new IllegalStateException("이미 비활성화된 초대 링크입니다.");
        }
        if (revokedAt == null) {
            throw new IllegalArgumentException("초대 링크 비활성화 시각은 필수입니다.");
        }

        this.active = false;
        this.revokedAt = revokedAt;
    }

    private static Project requireProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("프로젝트는 필수입니다.");
        }
        return project;
    }

    private static Member requireCreatedBy(Member createdBy) {
        if (createdBy == null) {
            throw new IllegalArgumentException("초대 링크 생성자는 필수입니다.");
        }
        return createdBy;
    }

    private static String requireTokenHash(String tokenHash) {
        if (tokenHash == null || !tokenHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException("초대 링크 토큰 해시는 SHA-256 형식이어야 합니다.");
        }
        return tokenHash;
    }
}
