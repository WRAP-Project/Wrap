package com.wrap.domain.schedule.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.dto.ScheduleUpdateRequest;
import com.wrap.domain.schedule.entity.Schedule;
import com.wrap.domain.schedule.repository.ScheduleRepository;
import com.wrap.global.exception.BusinessException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.lang.reflect.Constructor;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleServiceTest {

    private ScheduleRepository scheduleRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        scheduleService = new ScheduleService(
                scheduleRepository,
                memberRepository,
                projectRepository,
                projectMemberRepository
        );
    }

    @Test
    void updateScheduleAppliesOnlyProvidedFields() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "기존 제목",
                "기존 설명",
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                false
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        ScheduleUpdateRequest request = new ScheduleUpdateRequest(
                null,
                "수정 제목",
                null,
                null,
                null,
                null
        );

        ScheduleResponse response = scheduleService.update(1L, 1L, request);

        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.description()).isEqualTo("기존 설명");
        assertThat(response.startAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 14, 0));
        assertThat(response.endAt()).isEqualTo(LocalDateTime.of(2026, 7, 23, 15, 0));
        assertThat(response.shared()).isFalse();
    }

    @Test
    void projectOwnerCanDeleteSharedProjectSchedule() {
        Member creator = member(1L);
        Project project = project(10L);
        Schedule schedule = new Schedule(
                project,
                creator,
                "팀 일정",
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
                "팀 일정",
                null,
                LocalDateTime.of(2026, 7, 23, 14, 0),
                LocalDateTime.of(2026, 7, 23, 15, 0),
                true
        );
        ReflectionTestUtils.setField(schedule, "id", 1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertThatThrownBy(() -> scheduleService.delete(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void updateScheduleRejectsInvalidDateRangeAfterMerge() {
        Member creator = member(1L);
        Schedule schedule = new Schedule(
                null,
                creator,
                "기존 제목",
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
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
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
            throw new IllegalStateException("테스트 엔티티 생성에 실패했습니다.", exception);
        }
    }
}
