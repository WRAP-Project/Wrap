package com.wrap.domain.invitelink.repository;

import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectInviteLinkRepository extends JpaRepository<ProjectInviteLink, Long> {

    boolean existsByTokenHash(String tokenHash);

    @EntityGraph(attributePaths = {"project", "createdBy"})
    Optional<ProjectInviteLink> findByTokenHash(String tokenHash);

    @Query("select inviteLink.project.id from ProjectInviteLink inviteLink "
            + "where inviteLink.tokenHash = :tokenHash")
    Optional<Long> findProjectIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"project", "createdBy"})
    @Query("""
            select inviteLink
            from ProjectInviteLink inviteLink
            where inviteLink.tokenHash = :tokenHash
            """)
    Optional<ProjectInviteLink> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    boolean existsByProjectIdAndActiveTrueAndExpiresAtAfter(
            Long projectId,
            LocalDateTime now
    );

    @EntityGraph(attributePaths = {"createdBy"})
    List<ProjectInviteLink> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"project", "createdBy"})
    @Query("""
            select inviteLink
            from ProjectInviteLink inviteLink
            where inviteLink.id = :inviteLinkId
              and inviteLink.project.id = :projectId
            """)
    Optional<ProjectInviteLink> findByIdAndProjectIdForUpdate(
            @Param("inviteLinkId") Long inviteLinkId,
            @Param("projectId") Long projectId
    );
}
