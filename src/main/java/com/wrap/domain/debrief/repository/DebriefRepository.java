package com.wrap.domain.debrief.repository;

import com.wrap.domain.debrief.entity.Debrief;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebriefRepository extends JpaRepository<Debrief, Long> {

    Optional<Debrief> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}
