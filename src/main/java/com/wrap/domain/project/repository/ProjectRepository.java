package com.wrap.domain.project.repository;

import com.wrap.domain.project.entity.Project;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select project
            from Project project
            where project.id = :projectId
              and project.deletedAt is null
            """)
    Optional<Project> findByIdAndDeletedAtIsNullForUpdate(@Param("projectId") Long projectId);
}
