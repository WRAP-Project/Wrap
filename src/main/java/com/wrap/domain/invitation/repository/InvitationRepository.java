package com.wrap.domain.invitation.repository;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    @EntityGraph(attributePaths = {"project", "invitee"})
    List<Invitation> findAllByProjectIdAndInviteeIdAndStatus(
            Long projectId,
            Long inviteeId,
            InvitationStatus status
    );

    @EntityGraph(attributePaths = {"project", "inviter"})
    List<Invitation> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);

    @EntityGraph(attributePaths = {"project", "invitee"})
    List<Invitation> findAllByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("select invitation.project.id from Invitation invitation "
            + "where invitation.id = :invitationId")
    Optional<Long> findProjectIdById(@Param("invitationId") Long invitationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"project", "invitee"})
    @Query("select invitation from Invitation invitation where invitation.id = :invitationId")
    Optional<Invitation> findByIdForUpdate(@Param("invitationId") Long invitationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"project", "invitee"})
    @Query("""
            select invitation
            from Invitation invitation
            where invitation.project.id = :projectId
              and invitation.invitee.id = :inviteeId
              and invitation.status = :status
              and invitation.expiresAt > :now
            """)
    Optional<Invitation> findActiveByProjectIdAndInviteeIdForUpdate(
            @Param("projectId") Long projectId,
            @Param("inviteeId") Long inviteeId,
            @Param("status") InvitationStatus status,
            @Param("now") LocalDateTime now
    );

    long countByProjectIdAndStatusAndExpiresAtAfter(
            Long projectId,
            InvitationStatus status,
            LocalDateTime now
    );
}
