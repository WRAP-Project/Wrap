package com.wrap.domain.schedule.repository;

import com.wrap.domain.schedule.entity.Schedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByProjectId(Long projectId);

    List<Schedule> findByCreatorId(Long creatorId);
}
