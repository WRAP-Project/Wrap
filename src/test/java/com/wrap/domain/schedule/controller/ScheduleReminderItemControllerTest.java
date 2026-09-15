package com.wrap.domain.schedule.controller;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.schedule.dto.ReminderItemStatusResponse;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistSourceType;
import com.wrap.domain.schedule.dto.ScheduleReminderChecklistItemResponse.ReminderChecklistStatus;
import com.wrap.domain.schedule.service.ScheduleReminderItemService;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleReminderItemController.class)
@Import(SecurityConfig.class)
class ScheduleReminderItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleReminderItemService scheduleReminderItemService;

    @Test
    void updateStatusReturnsReminderItemStatus() throws Exception {
        ReminderItemStatusResponse response = ReminderItemStatusResponse.of(
                ReminderChecklistSourceType.TASK,
                2L,
                ReminderChecklistStatus.DONE
        );
        whenUpdateStatus(response);

        mockMvc.perform(patch("/projects/10/schedules/1/reminder-items/TASK/2/status")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceType").value("TASK"))
                .andExpect(jsonPath("$.data.sourceId").value(2))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.message").value("Reminder item status updated."));

        verify(scheduleReminderItemService).updateStatus(
                eq(1L),
                eq(10L),
                eq(1L),
                eq(ReminderChecklistSourceType.TASK),
                eq(2L),
                any()
        );
    }

    private void whenUpdateStatus(ReminderItemStatusResponse response) {
        org.mockito.Mockito.when(scheduleReminderItemService.updateStatus(
                eq(1L),
                eq(10L),
                eq(1L),
                eq(ReminderChecklistSourceType.TASK),
                eq(2L),
                any()
        )).thenReturn(response);
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
