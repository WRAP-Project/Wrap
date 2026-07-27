package com.wrap.domain.schedule.repository;

import com.wrap.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByProjectId(Long projectId);

    List<Schedule> findByCreatorId(Long creatorId);

    @Query("""
            select s from Schedule s
            where s.creator.id = :creatorId
              and (:projectId is null or s.project.id = :projectId)
              and (:fromAt is null or s.endAt >= :fromAt)
              and (:toAt is null or s.startAt < :toAt)
            order by s.startAt asc
            """)
    List<Schedule> findMySchedules(
            @Param("creatorId") Long creatorId,
            @Param("projectId") Long projectId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt
    );

    @Query("""
            select s from Schedule s
            where s.project.id = :projectId
              and s.shared = true
              and (:fromAt is null or s.endAt >= :fromAt)
              and (:toAt is null or s.startAt < :toAt)
            order by s.startAt asc
            """)
    List<Schedule> findSharedProjectSchedules(
            @Param("projectId") Long projectId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt
    );

    @Query("""
            select s from Schedule s
            where s.project.id = :projectId
              and s.shared = true
              and s.startAt >= :now
              and s.startAt < :until
            order by s.startAt asc
            """)
    List<Schedule> findUpcomingSharedProjectSchedules(
            @Param("projectId") Long projectId,
            @Param("now") LocalDateTime now,
            @Param("until") LocalDateTime until,
            Pageable pageable
    );
}
