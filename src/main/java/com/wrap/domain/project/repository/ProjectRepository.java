package com.wrap.domain.project.repository;

import com.wrap.domain.project.entity.Project;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByIdAndDeletedAtIsNull(Long projectId);
}
