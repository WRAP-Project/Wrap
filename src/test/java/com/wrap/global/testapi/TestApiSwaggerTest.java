package com.wrap.global.testapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestApiSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testApiReturnsSuccessMessage() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Swagger is working"));
    }

    @Test
    void swaggerApiDocsIncludeTestApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Wrap API"))
                .andExpect(jsonPath("$.paths['/api/test']").exists())
                .andExpect(jsonPath("$.paths['/schedules']").exists())
                .andExpect(jsonPath("$.paths['/schedules/me']").exists())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/schedules']").exists())
                .andExpect(jsonPath("$.paths['/schedules/{scheduleId}']").exists())
                .andExpect(jsonPath("$.paths['/schedules/{scheduleId}/check']").exists())
                .andExpect(jsonPath("$.paths['/schedules/{scheduleId}/uncheck']").exists())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/schedules/reminders']").exists())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/calendar/risk-checks']").exists())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/availability-requests']").exists())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/availability-requests/{availabilityRequestId}']").exists())
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/availability-requests/{availabilityRequestId}/me/response']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/availability-requests/{availabilityRequestId}/responses']"
                ).exists())    
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/availability-requests/{availabilityRequestId}/recommended-slots']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/availability-requests/{availabilityRequestId}/confirm']"
                ).exists())
                .andExpect(jsonPath("$.paths['/projects/{projectId}/invitations'].post").exists())
                .andExpect(jsonPath("$.paths['/invitations'].get").exists())
                .andExpect(jsonPath("$.components.schemas.InvitationCreateRequest").exists())
                .andExpect(jsonPath("$.components.schemas.InvitationResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ReceivedInvitationResponse").exists());
    }
}
