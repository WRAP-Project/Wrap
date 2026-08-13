package com.wrap.domain.availability.repository;

import com.wrap.domain.availability.entity.AvailabilityRequest;
import com.wrap.domain.availability.enums.AvailabilityRequestStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRequestRepository extends JpaRepository<AvailabilityRequest, Long> {

    Optional<AvailabilityRequest> findByIdAndProject_Id(Long availabilityRequestId, Long projectId);

    boolean existsByProject_IdAndStartDateAndEndDate(Long projectId, LocalDate startDate, LocalDate endDate);

    @Query("""
            select ar from AvailabilityRequest ar
            join fetch ar.project
            join fetch ar.creator creator
            join fetch creator.member
            where ar.project.id = :projectId
              and (:status is null or ar.status = :status)
              and (:from is null or ar.endDate >= :from)
              and (:to is null or ar.startDate <= :to)
            order by ar.startDate asc, ar.id asc
            """)
    List<AvailabilityRequest> findSummaries(
            @Param("projectId") Long projectId,
            @Param("status") AvailabilityRequestStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
