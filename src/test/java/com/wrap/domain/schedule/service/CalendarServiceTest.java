package com.wrap.domain.schedule.service;

import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CalendarServiceTest {

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(
                mock(TaskRepository.class),
                mock(ProjectMemberValidator.class)
        );
    }

    @Test
    void findRiskChecksRejectsInvalidDueDateRange() {
        assertThatThrownBy(() -> calendarService.findRiskChecks(
                1L,
                10L,
                null,
                null,
                LocalDate.of(2026, 7, 22),
                LocalDate.of(2026, 7, 15)
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }
}
