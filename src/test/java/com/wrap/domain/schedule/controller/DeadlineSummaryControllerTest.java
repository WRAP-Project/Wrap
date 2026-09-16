package com.wrap.domain.schedule.controller;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.schedule.dto.DeadlineSummaryHeaderResponse;
import com.wrap.domain.schedule.dto.DeadlineSummaryResponse;
import com.wrap.domain.schedule.dto.DeadlineSummaryStatsResponse;
import com.wrap.domain.schedule.service.DeadlineSummaryService;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeadlineSummaryController.class)
@Import(SecurityConfig.class)
class DeadlineSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeadlineSummaryService deadlineSummaryService;

    @Test
    void getDeadlineSummaryReturnsSummaries() throws Exception {
        when(deadlineSummaryService.getSummaries(1L, 10L, 1))
                .thenReturn(List.of(new DeadlineSummaryResponse(
                        new DeadlineSummaryHeaderResponse(
                                2L,
                                3,
                                "Deadline",
                                LocalDateTime.of(2026, 9, 30, 10, 0),
                                LocalDateTime.of(2026, 9, 30, 11, 0),
                                50
                        ),
                        new DeadlineSummaryStatsResponse(1, 2, 0, 1),
                        List.of(),
                        List.of(),
                        null
                )));

        mockMvc.perform(get("/projects/10/deadline-summary")
                        .with(user(memberDetails(1L)))
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].header.scheduleId").value(2))
                .andExpect(jsonPath("$.data[0].stats.checklistDone").value(1))
                .andExpect(jsonPath("$.message").value("Deadline summaries retrieved."));

        verify(deadlineSummaryService).getSummaries(1L, 10L, 1);
    }

    private MemberDetails memberDetails(Long memberId) {
        Member member = Member.builder()
                .email("member@example.com")
                .password("encoded-password")
                .nickname("member")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return new MemberDetails(member);
    }
}
