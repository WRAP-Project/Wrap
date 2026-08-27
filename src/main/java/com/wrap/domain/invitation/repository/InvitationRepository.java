package com.wrap.domain.invitation.repository;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    boolean existsByProjectIdAndInviteeIdAndStatus(
            Long projectId,
            Long inviteeId,
            InvitationStatus status
    );

    @EntityGraph(attributePaths = {"project", "inviter"})
    List<Invitation> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"project", "invitee"})
    @Query("select invitation from Invitation invitation where invitation.id = :invitationId")
    Optional<Invitation> findByIdForUpdate(@Param("invitationId") Long invitationId);
}
