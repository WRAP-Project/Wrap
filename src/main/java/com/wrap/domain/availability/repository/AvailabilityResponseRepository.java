package com.wrap.domain.availability.repository;

import com.wrap.domain.availability.entity.AvailabilityResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityResponseRepository extends JpaRepository<AvailabilityResponse, Long> {

    Optional<AvailabilityResponse> findByAvailabilityRequest_IdAndProjectMember_Id(
            Long availabilityRequestId,
            Long projectMemberId
    );

    long countByAvailabilityRequest_Id(Long availabilityRequestId);

    @EntityGraph(attributePaths = {"projectMember", "projectMember.member", "slots"})
    List<AvailabilityResponse> findAllByAvailabilityRequest_Id(Long availabilityRequestId);
}
