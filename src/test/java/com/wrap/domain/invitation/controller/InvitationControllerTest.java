package com.wrap.domain.invitation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.invitation.dto.request.InvitationCreateRequest;
import com.wrap.domain.invitation.dto.response.InvitationResponse;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.invitation.service.InvitationService;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvitationController.class)
@Import(SecurityConfig.class)
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void createReturnsCreatedAndUsesAuthenticatedMemberId() throws Exception {
        InvitationResponse response = InvitationResponse.builder()
                .invitationId(100L)
                .projectId(10L)
                .projectName("Wrap")
                .inviteeMemberId(2L)
                .inviteeEmail("invitee@example.com")
                .role(ProjectMemberRole.MEMBER)
                .status(InvitationStatus.INVITED)
                .createdAt(LocalDateTime.of(2026, 8, 16, 10, 0))
                .build();
        when(invitationService.create(
                eq(1L),
                eq(10L),
                any(InvitationCreateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/projects/10/invitations")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invitationId").value(100))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.projectName").value("Wrap"))
                .andExpect(jsonPath("$.data.inviteeMemberId").value(2))
                .andExpect(jsonPath("$.data.inviteeEmail").value("invitee@example.com"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("INVITED"))
                .andExpect(jsonPath("$.message").value("Project invitation created."));

        verify(invitationService).create(
                eq(1L),
                eq(10L),
                any(InvitationCreateRequest.class)
        );
    }

    @Test
    void createWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/projects/10/invitations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void createWithoutEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/projects/10/invitations")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("email"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void createWithInvalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/projects/10/invitations")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("email"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void createWithoutRoleReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/projects/10/invitations")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("role"));

        verifyNoInteractions(invitationService);
    }

    private String validRequest() {
        return """
                {
                  "email": "invitee@example.com",
                  "role": "MEMBER"
                }
                """;
    }

    private MemberDetails memberDetails(Long memberId) {
        Member member = Member.builder()
                .email("member" + memberId + "@example.com")
                .password("password1234")
                .nickname("member" + memberId)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return new MemberDetails(member);
    }
}
