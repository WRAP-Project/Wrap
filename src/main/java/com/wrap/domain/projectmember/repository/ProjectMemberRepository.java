package com.wrap.domain.projectmember.repository;

import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findAllByProjectIdAndStatus(
            Long projectId,
            ProjectMemberStatus status
    );

    @EntityGraph(attributePaths = "project")
    List<ProjectMember> findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
            Long memberId,
            ProjectMemberStatus status
    );

    @EntityGraph(attributePaths = "project")
    List<ProjectMember> findAllByMemberIdAndStatusAndProjectStatusAndProjectDeletedAtIsNull(
            Long memberId,
            ProjectMemberStatus status,
            ProjectStatus projectStatus
    );

    @Query("""
            select projectMember
            from ProjectMember projectMember
            join fetch projectMember.project project
            join fetch projectMember.member member
            where project.id in :projectIds
              and projectMember.status = :status
            order by project.id asc, projectMember.joinedAt asc, projectMember.id asc
            """)
    List<ProjectMember> findAllByProjectIdsAndStatusWithMember(
            @Param("projectIds") List<Long> projectIds,
            @Param("status") ProjectMemberStatus status
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
