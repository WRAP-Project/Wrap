package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleReminderResponse;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private ScheduleRepository scheduleRepository;
    private MemberRepository memberRepository;
    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepository.class);
        memberRepository = mock(MemberRepository.class);
        projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        scheduleService = new ScheduleService(
                scheduleRepository,
                memberRepository,
                projectRepository,
                projectMemberRepository
        );
    }

    @Test
    void createPrivateScheduleIgnoresProjectId() {
        Member creator = member(1L);
        ScheduleCreateRequest request = new ScheduleCreateRequest(
                10L,
                "Private schedule",
                "Project id should be ignored.",
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        when(memberRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", 1L);
            return schedule;
        });

        ScheduleResponse response = scheduleService.create(1L, request);

        assertThat(response.projectId()).isNull();
        assertThat(response.shared()).isFalse();
    }

    @Test
    void updateScheduleAppliesOnlyProvidedFields() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "Old title",
                "Old description",
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                null,
                "New title",
                null,
                null,
                null,
                null
        );

        ScheduleResponse response = scheduleService.update(1L, 1L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("Old description");
        assertThat(response.startAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 14, 0));
        assertThat(response.endAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 15, 0));
        assertThat(response.shared()).isFalse();
    }

    @Test
    void updatePrivateScheduleRemovesProject() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                10L,
                null,
                null,
                null,
                null,
                false
        );

        ScheduleResponse response = scheduleService.update(1L, 1L, request);

        assertThat(response.projectId()).isNull();
        assertThat(response.shared()).isFalse();
    }

    @Test
    void projectOwnerCanDeleteSharedProjectSchedule() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(projectMemberRepository.existsByMemberIdAndProjectIdAndRoleAndStatus(
                2L,
                10L,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);

        scheduleService.delete(2L, 1L);

        verify(scheduleRepository).delete(schedule);
    }

    @Test
    void nonOwnerCannotDeleteAnotherMembersSchedule() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Shared schedule",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.delete(2L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void updateScheduleRejectsInvalidDateRangeAfterMerge() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "Old title",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                null,
                null,
                null,
                LocalDateTime.of(2026, 7, 23, 16, 0),
                null,
                null
        );

        assertThatThrownBy(() -> scheduleService.update(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    void remindersUseEndAtAsDeadline() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "Deadline schedule",
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(2),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(projectMemberRepository.existsByMemberIdAndProjectIdAndStatus(
                1L,
                10L,
                ProjectMemberStatus.JOINED
        )).thenReturn(true);
        when(scheduleRepository.findUpcomingSharedProjectSchedules(eq(10L), any(), any(), any()))
                .thenReturn(List.of(schedule));

        List<ScheduleReminderResponse> responses = scheduleService.findReminders(1L, 10L, 7, 5);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).daysLeft()).isEqualTo(2);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(scheduleRepository).findUpcomingSharedProjectSchedules(
                eq(10L),
                nowCaptor.capture(),
                any(),
                any()
        );
        assertThat(schedule.getEndAt()).isAfter(nowCaptor.getValue());
    }

    private Member member(Long id) {
        Member member = instantiate(Member.class);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Project project(Long id) {
        Project project = instantiate(Project.class);
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity.", exception);
        }
    }
}
