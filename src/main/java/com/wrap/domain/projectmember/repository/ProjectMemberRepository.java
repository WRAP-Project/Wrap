package com.wrap.domain.projectmember.repository;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    List<ProjectMember> findByProjectId(Long projectId);

    List<ProjectMember> findByMemberId(Long memberId);

    Optional<ProjectMember> findByMemberIdAndProjectId(Long memberId, Long projectId);

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
}
