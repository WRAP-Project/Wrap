package com.wrap.domain.availability.service;

import com.wrap.domain.availability.dto.AvailabilityConfirmRequest;
import com.wrap.domain.availability.dto.AvailabilityRequestCreateRequest;
import com.wrap.domain.availability.dto.AvailabilityRequestDetailResponse;
import com.wrap.domain.availability.dto.AvailabilityRequestSummaryResponse;
import com.wrap.domain.availability.dto.AvailabilityResponseUpsertRequest;
import com.wrap.domain.availability.dto.AvailabilityResponsesResponse;
import com.wrap.domain.availability.dto.AvailabilitySlotRequest;
import com.wrap.domain.availability.dto.AvailabilitySlotResponse;
import com.wrap.domain.availability.dto.BusySlotResponse;
import com.wrap.domain.availability.dto.MemberAvailabilityResponse;
import com.wrap.domain.availability.dto.MyAvailabilityResponse;
import com.wrap.domain.availability.dto.RecommendedSlotResponse;
import com.wrap.domain.availability.entity.AvailabilityRequest;
import com.wrap.domain.availability.entity.AvailabilityResponse;
import com.wrap.domain.availability.entity.AvailabilitySlot;
import com.wrap.domain.availability.enums.AvailabilityRequestStatus;
import com.wrap.domain.availability.repository.AvailabilityRequestRepository;
import com.wrap.domain.availability.repository.AvailabilityResponseRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final int SUPPORTED_SLOT_UNIT_MINUTES = 60;

    private final AvailabilityRequestRepository availabilityRequestRepository;
    private final AvailabilityResponseRepository availabilityResponseRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberValidator projectMemberValidator;

    @Transactional
    public AvailabilityRequestDetailResponse create(
            Long memberId,
            Long projectId,
            AvailabilityRequestCreateRequest request
    ) {
        Project project = projectMemberValidator.findActiveProject(projectId);
        ProjectMember creator = projectMemberValidator.findJoinedMember(memberId, projectId);
        validateRequestRange(request.startDate(), request.endDate());
        validateSlotUnit(request.slotUnitMinutes());
        if (availabilityRequestRepository.existsByProject_IdAndStartDateAndEndDate(
                projectId,
                request.startDate(),
                request.endDate()
        )) {
            throw new CustomException(ErrorCode.AVAILABILITY_REQUEST_DUPLICATED);
        }

        AvailabilityRequest availabilityRequest = AvailabilityRequest.create(
                project,
                creator,
                request.title(),
                request.description(),
                request.startDate(),
                request.endDate(),
                request.slotUnitMinutes()
        );
        AvailabilityRequest savedRequest = availabilityRequestRepository.save(availabilityRequest);
        return AvailabilityRequestDetailResponse.of(savedRequest, countJoinedMembers(projectId), 0);
    }

    public List<AvailabilityRequestSummaryResponse> findAll(
            Long memberId,
            Long projectId,
            AvailabilityRequestStatus status,
            LocalDate from,
            LocalDate to
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        validateDateRange(from, to);
        return availabilityRequestRepository.findSummaries(projectId, status, from, to)
                .stream()
                .map(AvailabilityRequestSummaryResponse::from)
                .toList();
    }

    public AvailabilityRequestDetailResponse findDetail(Long memberId, Long projectId, Long availabilityRequestId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        AvailabilityRequest availabilityRequest = findRequest(projectId, availabilityRequestId);
        return AvailabilityRequestDetailResponse.of(
                availabilityRequest,
                countJoinedMembers(projectId),
                availabilityResponseRepository.countByAvailabilityRequest_Id(availabilityRequestId)
        );
    }

    public MyAvailabilityResponse findMyResponse(Long memberId, Long projectId, Long availabilityRequestId) {
        ProjectMember projectMember = projectMemberValidator.findJoinedMember(memberId, projectId);
        AvailabilityRequest availabilityRequest = findRequest(projectId, availabilityRequestId);
        validateNotCanceled(availabilityRequest);

        List<BusySlotResponse> busySlots = scheduleRepository.findMySchedules(
                        memberId,
                        null,
                        toStartOfDay(availabilityRequest.getStartDate()),
                        toExclusiveEnd(availabilityRequest.getEndDate())
                )
                .stream()
                .map(BusySlotResponse::fromSchedule)
                .toList();
        List<AvailabilitySlotResponse> selectedSlots = availabilityResponseRepository
                .findByAvailabilityRequest_IdAndProjectMember_Id(availabilityRequestId, projectMember.getId())
                .map(AvailabilityResponse::getSlots)
                .orElse(List.of())
                .stream()
                .map(AvailabilitySlotResponse::from)
                .toList();

        return MyAvailabilityResponse.of(availabilityRequest, busySlots, selectedSlots);
    }

    @Transactional
    public MyAvailabilityResponse upsertMyResponse(
            Long memberId,
            Long projectId,
            Long availabilityRequestId,
            AvailabilityResponseUpsertRequest request
    ) {
        ProjectMember projectMember = projectMemberValidator.findJoinedMember(memberId, projectId);
        AvailabilityRequest availabilityRequest = findRequest(projectId, availabilityRequestId);
        validateOpen(availabilityRequest);

        List<AvailabilitySlot> slots = request.slots()
                .stream()
                .map(slot -> createValidatedSlot(availabilityRequest, slot))
                .toList();
        AvailabilityResponse response = availabilityResponseRepository
                .findByAvailabilityRequest_IdAndProjectMember_Id(availabilityRequestId, projectMember.getId())
                .orElseGet(() -> AvailabilityResponse.create(availabilityRequest, projectMember, LocalDateTime.now()));
        response.replaceSlots(slots, LocalDateTime.now());
        availabilityResponseRepository.save(response);

        return findMyResponse(memberId, projectId, availabilityRequestId);
    }

    public AvailabilityResponsesResponse findResponses(Long memberId, Long projectId, Long availabilityRequestId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        findRequest(projectId, availabilityRequestId);

        Map<Long, AvailabilityResponse> responseByProjectMemberId = new HashMap<>();
        availabilityResponseRepository.findAllByAvailabilityRequest_Id(availabilityRequestId)
                .forEach(response -> responseByProjectMemberId.put(response.getProjectMember().getId(), response));

        List<MemberAvailabilityResponse> members = projectMemberRepository
                .findAllByProjectIdAndStatus(projectId, ProjectMemberStatus.JOINED)
                .stream()
                .map(projectMember -> toMemberAvailability(projectMember, responseByProjectMemberId))
                .toList();

        return new AvailabilityResponsesResponse(availabilityRequestId, members);
    }

    public List<RecommendedSlotResponse> findRecommendedSlots(
            Long memberId,
            Long projectId,
            Long availabilityRequestId
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        findRequest(projectId, availabilityRequestId);
        return calculateRecommendedSlots(projectId, availabilityRequestId);
    }

    @Transactional
    public ScheduleResponse confirm(
            Long memberId,
            Long projectId,
            Long availabilityRequestId,
            AvailabilityConfirmRequest request
    ) {
        ProjectMember confirmer = projectMemberValidator.findJoinedMember(memberId, projectId);
        AvailabilityRequest availabilityRequest = findRequest(projectId, availabilityRequestId);
        validateConfirmPermission(availabilityRequest, confirmer);
        validateOpen(availabilityRequest);
        validateDateTimeRange(request.startAt(), request.endAt());
        validateSlot(availabilityRequest, request.startAt(), request.endAt());

        SlotKey selectedSlot = new SlotKey(request.startAt(), request.endAt());
        boolean recommended = calculateRecommendedSlots(projectId, availabilityRequestId)
                .stream()
                .anyMatch(slot -> slot.startAt().equals(selectedSlot.startAt())
                        && slot.endAt().equals(selectedSlot.endAt()));
        if (!recommended) {
            throw new CustomException(ErrorCode.INVALID_RECOMMENDED_SLOT);
        }

        Schedule schedule = new Schedule(
                availabilityRequest.getProject(),
                confirmer.getMember(),
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                true
        );
        Schedule savedSchedule = scheduleRepository.save(schedule);
        availabilityRequest.confirm(savedSchedule);

        return ScheduleResponse.from(savedSchedule);
    }

    private MemberAvailabilityResponse toMemberAvailability(
            ProjectMember projectMember,
            Map<Long, AvailabilityResponse> responseByProjectMemberId
    ) {
        AvailabilityResponse response = responseByProjectMemberId.get(projectMember.getId());
        List<AvailabilitySlotResponse> slots = response == null
                ? List.of()
                : response.getSlots().stream().map(AvailabilitySlotResponse::from).toList();
        return new MemberAvailabilityResponse(
                projectMember.getId(),
                projectMember.getMember().getNickname(),
                response != null,
                slots
        );
    }

    private List<RecommendedSlotResponse> calculateRecommendedSlots(Long projectId, Long availabilityRequestId) {
        int totalMemberCount = countJoinedMembers(projectId);
        Map<SlotKey, Set<Long>> projectMemberIdsBySlot = new HashMap<>();
        availabilityResponseRepository.findAllByAvailabilityRequest_Id(availabilityRequestId)
                .forEach(response -> {
                    Set<SlotKey> uniqueSlots = new HashSet<>();
                    response.getSlots().forEach(slot -> uniqueSlots.add(new SlotKey(slot.getStartAt(), slot.getEndAt())));
                    uniqueSlots.forEach(slot -> projectMemberIdsBySlot
                            .computeIfAbsent(slot, ignored -> new HashSet<>())
                            .add(response.getProjectMember().getId()));
                });

        return projectMemberIdsBySlot.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() == totalMemberCount)
                .map(entry -> new RecommendedSlotResponse(
                        entry.getKey().startAt(),
                        entry.getKey().endAt(),
                        entry.getValue().size(),
                        totalMemberCount
                ))
                .sorted(Comparator.comparing(RecommendedSlotResponse::startAt))
                .toList();
    }

    private AvailabilityRequest findRequest(Long projectId, Long availabilityRequestId) {
        return availabilityRequestRepository.findByIdAndProject_Id(availabilityRequestId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.AVAILABILITY_REQUEST_NOT_FOUND));
    }

    private AvailabilitySlot createValidatedSlot(
            AvailabilityRequest availabilityRequest,
            AvailabilitySlotRequest slot
    ) {
        validateDateTimeRange(slot.startAt(), slot.endAt());
        validateSlot(availabilityRequest, slot.startAt(), slot.endAt());
        return new AvailabilitySlot(slot.startAt(), slot.endAt());
    }

    private void validateConfirmPermission(AvailabilityRequest availabilityRequest, ProjectMember confirmer) {
        if (!availabilityRequest.getCreator().getId().equals(confirmer.getId()) && !confirmer.isOwner()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateOpen(AvailabilityRequest availabilityRequest) {
        if (availabilityRequest.getStatus() == AvailabilityRequestStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.AVAILABILITY_REQUEST_ALREADY_CONFIRMED);
        }
        if (availabilityRequest.getStatus() == AvailabilityRequestStatus.CANCELED) {
            throw new CustomException(ErrorCode.AVAILABILITY_REQUEST_CANCELED);
        }
    }

    private void validateNotCanceled(AvailabilityRequest availabilityRequest) {
        if (availabilityRequest.getStatus() == AvailabilityRequestStatus.CANCELED) {
            throw new CustomException(ErrorCode.AVAILABILITY_REQUEST_CANCELED);
        }
    }

    private void validateRequestRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_AVAILABILITY_RANGE);
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateDateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateSlotUnit(Integer slotUnitMinutes) {
        if (slotUnitMinutes == null || slotUnitMinutes != SUPPORTED_SLOT_UNIT_MINUTES) {
            throw new CustomException(ErrorCode.INVALID_SLOT_UNIT);
        }
    }

    private void validateSlot(AvailabilityRequest availabilityRequest, LocalDateTime startAt, LocalDateTime endAt) {
        validateSlotUnit(availabilityRequest.getSlotUnitMinutes());
        boolean alignedToHour = startAt.getMinute() == 0
                && startAt.getSecond() == 0
                && startAt.getNano() == 0
                && endAt.getMinute() == 0
                && endAt.getSecond() == 0
                && endAt.getNano() == 0;
        boolean unitMatched = Duration.between(startAt, endAt).toMinutes() == availabilityRequest.getSlotUnitMinutes();
        if (!alignedToHour || !unitMatched) {
            throw new CustomException(ErrorCode.INVALID_SLOT_UNIT);
        }

        LocalDateTime rangeStart = availabilityRequest.getStartDate().atStartOfDay();
        LocalDateTime rangeEndExclusive = availabilityRequest.getEndDate().plusDays(1).atStartOfDay();
        if (startAt.isBefore(rangeStart) || endAt.isAfter(rangeEndExclusive)) {
            throw new CustomException(ErrorCode.INVALID_SLOT_RANGE);
        }
    }

    private int countJoinedMembers(Long projectId) {
        return projectMemberRepository.findAllByProjectIdAndStatus(projectId, ProjectMemberStatus.JOINED).size();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    private LocalDateTime toExclusiveEnd(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }

    private record SlotKey(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
