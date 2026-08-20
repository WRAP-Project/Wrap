package com.wrap.domain.invitation.repository;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    boolean existsByProjectIdAndInviteeIdAndStatus(
            Long projectId,
            Long inviteeId,
            InvitationStatus status
    );

    @EntityGraph(attributePaths = {"project", "inviter"})
    List<Invitation> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);
}
