package com.wrap.domain.milestone.repository;

import com.wrap.domain.milestone.entity.Milestone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByProjectId(Long projectId);
}
