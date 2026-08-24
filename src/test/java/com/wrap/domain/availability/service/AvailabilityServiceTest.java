package com.wrap.domain.availability.service;

import com.wrap.domain.availability.dto.AvailabilityConfirmRequest;
import com.wrap.domain.availability.dto.AvailabilityRequestCreateRequest;
import com.wrap.domain.availability.dto.AvailabilityResponseUpsertRequest;
import com.wrap.domain.availability.dto.AvailabilitySlotRequest;
import com.wrap.domain.availability.dto.RecommendedSlotResponse;
import com.wrap.domain.availability.entity.AvailabilityRequest;
import com.wrap.domain.availability.entity.AvailabilityResponse;
import com.wrap.domain.availability.entity.AvailabilitySlot;
import com.wrap.domain.availability.repository.AvailabilityRequestRepository;
import com.wrap.domain.availability.repository.AvailabilityResponseRepository;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityServiceTest {

    private AvailabilityRequestRepository availabilityRequestRepository;
    private AvailabilityResponseRepository availabilityResponseRepository;
    private ScheduleRepository scheduleRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectMemberValidator projectMemberValidator;
    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityRequestRepository = mock(AvailabilityRequestRepository.class);
        availabilityResponseRepository = mock(AvailabilityResponseRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        projectMemberValidator = mock(ProjectMemberValidator.class);
        availabilityService = new AvailabilityService(
                availabilityRequestRepository,
                availabilityResponseRepository,
                scheduleRepository,
                projectMemberRepository,
                projectMemberValidator
        );
    }

    @Test
    void createRejectsUnsupportedSlotUnit() {
        Project project = project(10L);
        ProjectMember creator = projectMember(1L, 1L, project, ProjectMemberRole.MEMBER);
        when(projectMemberValidator.findActiveProject(10L)).thenReturn(project);
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(creator);

        AvailabilityRequestCreateRequest request = new AvailabilityRequestCreateRequest(
                "Meeting",
                null,
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 22),
                30
        );

        assertThatThrownBy(() -> availabilityService.create(1L, 10L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_SLOT_UNIT));
    }

    @Test
    void upsertMyResponseRejectsSlotOutsideRequestRange() {
        Project project = project(10L);
        ProjectMember member = projectMember(1L, 1L, project, ProjectMemberRole.MEMBER);
        AvailabilityRequest availabilityRequest = availabilityRequest(100L, project, member);
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(member);
        when(availabilityRequestRepository.findByIdAndProject_Id(100L, 10L))
                .thenReturn(Optional.of(availabilityRequest));

        AvailabilityResponseUpsertRequest request = new AvailabilityResponseUpsertRequest(List.of(
                new AvailabilitySlotRequest(
                        LocalDateTime.of(2026, 7, 23, 10, 0),
                        LocalDateTime.of(2026, 7, 23, 11, 0)
                )
        ));

        assertThatThrownBy(() -> availabilityService.upsertMyResponse(1L, 10L, 100L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_SLOT_RANGE));
    }

    @Test
    void recommendedSlotsOnlyContainSlotsEveryJoinedMemberSelected() {
        Project project = project(10L);
        ProjectMember memberA = projectMember(1L, 1L, project, ProjectMemberRole.MEMBER);
        ProjectMember memberB = projectMember(2L, 2L, project, ProjectMemberRole.MEMBER);
        AvailabilityRequest availabilityRequest = availabilityRequest(100L, project, memberA);
        AvailabilityResponse responseA = availabilityResponse(1L, availabilityRequest, memberA, List.of(
                slot(LocalDateTime.of(2026, 7, 15, 14, 0), LocalDateTime.of(2026, 7, 15, 15, 0)),
                slot(LocalDateTime.of(2026, 7, 15, 16, 0), LocalDateTime.of(2026, 7, 15, 17, 0))
        ));
        AvailabilityResponse responseB = availabilityResponse(2L, availabilityRequest, memberB, List.of(
                slot(LocalDateTime.of(2026, 7, 15, 14, 0), LocalDateTime.of(2026, 7, 15, 15, 0))
        ));
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(memberA);
        when(availabilityRequestRepository.findByIdAndProject_Id(100L, 10L))
                .thenReturn(Optional.of(availabilityRequest));
        when(projectMemberRepository.findAllByProjectIdAndStatus(10L, ProjectMemberStatus.JOINED))
                .thenReturn(List.of(memberA, memberB));
        when(availabilityResponseRepository.findAllByAvailabilityRequest_Id(100L))
                .thenReturn(List.of(responseA, responseB));

        List<RecommendedSlotResponse> responses = availabilityService.findRecommendedSlots(1L, 10L, 100L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).startAt()).isEqualTo(LocalDateTime.of(2026, 7, 15, 14, 0));
        assertThat(responses.get(0).availableCount()).isEqualTo(2);
        assertThat(responses.get(0).totalMemberCount()).isEqualTo(2);
    }

    @Test
    void confirmCreatesSharedScheduleAndConfirmsRequest() {
        Project project = project(10L);
        ProjectMember memberA = projectMember(1L, 1L, project, ProjectMemberRole.MEMBER);
        ProjectMember memberB = projectMember(2L, 2L, project, ProjectMemberRole.MEMBER);
        AvailabilityRequest availabilityRequest = availabilityRequest(100L, project, memberA);
        AvailabilityResponse responseA = availabilityResponse(1L, availabilityRequest, memberA, List.of(
                slot(LocalDateTime.of(2026, 7, 15, 14, 0), LocalDateTime.of(2026, 7, 15, 15, 0))
        ));
        AvailabilityResponse responseB = availabilityResponse(2L, availabilityRequest, memberB, List.of(
                slot(LocalDateTime.of(2026, 7, 15, 14, 0), LocalDateTime.of(2026, 7, 15, 15, 0))
        ));
        when(projectMemberValidator.findJoinedMember(1L, 10L)).thenReturn(memberA);
        when(availabilityRequestRepository.findByIdAndProject_Id(100L, 10L))
                .thenReturn(Optional.of(availabilityRequest));
        when(projectMemberRepository.findAllByProjectIdAndStatus(10L, ProjectMemberStatus.JOINED))
                .thenReturn(List.of(memberA, memberB));
        when(availabilityResponseRepository.findAllByAvailabilityRequest_Id(100L))
                .thenReturn(List.of(responseA, responseB));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 200L);
            return schedule;
        });

        ScheduleResponse response = availabilityService.confirm(
                1L,
                10L,
                100L,
                new AvailabilityConfirmRequest(
                        "Team meeting",
                        "Confirmed from availability request.",
                        LocalDateTime.of(2026, 7, 15, 14, 0),
                        LocalDateTime.of(2026, 7, 15, 15, 0)
                )
        );

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.shared()).isTrue();
        assertThat(availabilityRequest.getStatus().name()).isEqualTo("CONFIRMED");
        assertThat(availabilityRequest.getConfirmedSchedule().getId()).isEqualTo(200L);
    }

    private AvailabilityRequest availabilityRequest(Long id, Project project, ProjectMember creator) {
        AvailabilityRequest request = AvailabilityRequest.create(
                project,
                creator,
                "Meeting",
                null,
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 22),
                60
        );
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private AvailabilityResponse availabilityResponse(
            Long id,
            AvailabilityRequest request,
            ProjectMember projectMember,
            List<AvailabilitySlot> slots
    ) {
        AvailabilityResponse response = AvailabilityResponse.create(request, projectMember, LocalDateTime.now());
        ReflectionTestUtils.setField(response, "id", id);
        response.replaceSlots(slots, LocalDateTime.now());
        return response;
    }

    private AvailabilitySlot slot(LocalDateTime startAt, LocalDateTime endAt) {
        return new AvailabilitySlot(startAt, endAt);
    }

    private Project project(Long id) {
        Project project = Project.create("Project", null, null, null, null, null, Project.DEFAULT_COLOR);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private ProjectMember projectMember(Long id, Long memberId, Project project, ProjectMemberRole role) {
        Member member = Member.builder()
                .email("member" + memberId + "@wrap.com")
                .password("password")
                .nickname("member" + memberId)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        ProjectMember projectMember = ProjectMember.join(member, project, role, LocalDateTime.now());
        ReflectionTestUtils.setField(projectMember, "id", id);
        return projectMember;
    }
}
