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
                .andExpect(jsonPath("$.components.schemas.ReceivedInvitationResponse").exists())
                .andExpect(jsonPath("$.components.securitySchemes.sessionAuth.in")
                        .value("cookie"))
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/invite-links'].post.summary"
                ).value("프로젝트 초대 링크 생성"))
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/invite-links'].post.responses['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/invite-links'].post.responses['409']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/invite-links'].post.responses['500']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/invite-links'].get.summary"
                ).value("프로젝트 초대 링크 목록 조회"))
                .andExpect(jsonPath(
                        "$.paths['/projects/{projectId}/invite-links/{inviteLinkId}'].delete"
                ).exists())
                .andExpect(jsonPath("$.paths['/invite-links/{token}'].get.summary")
                        .value("초대 링크 프로젝트 정보 조회"))
                .andExpect(jsonPath("$.paths['/invite-links/{token}'].get.security")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/invite-links/{token}/join'].post.summary")
                        .value("초대 링크를 통한 프로젝트 참여"))
                .andExpect(jsonPath(
                        "$.paths['/invite-links/{token}/join'].post.security[0].sessionAuth"
                ).exists())
                .andExpect(jsonPath("$.components.schemas.ProjectInviteLinkResponse")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ProjectInviteLinkSummaryResponse")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ProjectInviteLinkInfoResponse")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ProjectInviteJoinResponse")
                        .exists());
    }
}
