package com.wrap.domain.schedule.repository;

import com.wrap.domain.schedule.entity.ScheduleCheck;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleCheckRepository extends JpaRepository<ScheduleCheck, Long> {

    Optional<ScheduleCheck> findByScheduleIdAndMemberId(Long scheduleId, Long memberId);

    boolean existsByScheduleIdAndMemberId(Long scheduleId, Long memberId);
}
