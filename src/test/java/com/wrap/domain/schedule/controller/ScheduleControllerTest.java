package com.wrap.domain.schedule.controller;

import com.wrap.domain.schedule.dto.ScheduleCreateRequest;
import com.wrap.domain.schedule.dto.ScheduleResponse;
import com.wrap.domain.schedule.service.ScheduleService;
import com.wrap.global.auth.SessionMemberResolver;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScheduleControllerTest {

    private ScheduleService scheduleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        scheduleService = Mockito.mock(ScheduleService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ScheduleController(scheduleService, new SessionMemberResolver()))
                .build();
    }

    @Test
    void createScheduleUsesSessionMemberId() throws Exception {
        LocalDateTime startAt = LocalDateTime.of(2026, 7, 23, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 23, 15, 0);
        ScheduleResponse response = new ScheduleResponse(
                1L,
                10L,
                1L,
                "포스터 최종 회의",
                "최종 시안 검토",
                startAt,
                endAt,
                true
        );

        when(scheduleService.create(eq(1L), any(ScheduleCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/schedules")
                        .sessionAttr(SessionMemberResolver.MEMBER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": 10,
                                  "title": "포스터 최종 회의",
                                  "description": "최종 시안 검토",
                                  "startAt": "2026-07-23T14:00:00",
                                  "endAt": "2026-07-23T15:00:00",
                                  "shared": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.creatorId").value(1));

        verify(scheduleService).create(eq(1L), any(ScheduleCreateRequest.class));
    }
}
