package com.wrap.domain.projectmember.repository;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectIdAndStatus(
            Long projectId,
            ProjectMemberStatus status
    );

    List<ProjectMember> findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
            Long memberId,
            ProjectMemberStatus status
    );

    Optional<ProjectMember> findByIdAndProjectId(
            Long projectMemberId,
            Long projectId
    );

    Optional<ProjectMember> findByMemberIdAndProjectId(Long memberId, Long projectId);

    Optional<ProjectMember> findByMemberIdAndProjectIdAndStatus(
            Long memberId,
            Long projectId,
            ProjectMemberStatus status
    );

    boolean existsByMemberIdAndProjectId(Long memberId, Long projectId);

    boolean existsByMemberIdAndProjectIdAndStatus(
            Long memberId,
            Long projectId,
            ProjectMemberStatus status
    );

    boolean existsByMemberIdAndProjectIdAndRoleAndStatus(
            Long memberId,
            Long projectId,
            ProjectMemberRole role,
            ProjectMemberStatus status
    );

    long countByProjectIdAndRoleAndStatus(
            Long projectId,
            ProjectMemberRole role,
            ProjectMemberStatus status
    );
}
