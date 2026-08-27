package com.wrap.domain.invitation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.invitation.dto.request.InvitationCreateRequest;
import com.wrap.domain.invitation.dto.response.InvitationResponse;
import com.wrap.domain.invitation.dto.response.ReceivedInvitationResponse;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.invitation.service.InvitationService;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import java.time.LocalDateTime;
import java.util.List;
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
    void getReceivedInvitationsReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        ReceivedInvitationResponse response = ReceivedInvitationResponse.builder()
                .invitationId(100L)
                .projectId(10L)
                .projectName("Wrap")
                .inviterNickname("owner")
                .role(ProjectMemberRole.MEMBER)
                .status(InvitationStatus.INVITED)
                .createdAt(LocalDateTime.of(2026, 8, 18, 10, 0))
                .build();
        when(invitationService.getReceivedInvitations(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/invitations")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].invitationId").value(100))
                .andExpect(jsonPath("$.data[0].projectId").value(10))
                .andExpect(jsonPath("$.data[0].projectName").value("Wrap"))
                .andExpect(jsonPath("$.data[0].inviterNickname").value("owner"))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[0].status").value("INVITED"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-08-18T10:00:00"))
                .andExpect(jsonPath("$.message")
                        .value("Received project invitations retrieved."));

        verify(invitationService).getReceivedInvitations(1L);
    }

    @Test
    void getReceivedInvitationsReturnsEmptyList() throws Exception {
        when(invitationService.getReceivedInvitations(1L)).thenReturn(List.of());

        mockMvc.perform(get("/invitations")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(invitationService).getReceivedInvitations(1L);
    }

    @Test
    void getReceivedInvitationsWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/invitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void getSentInvitationsReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        InvitationResponse response = InvitationResponse.builder()
                .invitationId(100L)
                .projectId(10L)
                .projectName("Wrap")
                .inviteeMemberId(2L)
                .inviteeEmail("invitee@example.com")
                .role(ProjectMemberRole.MEMBER)
                .status(InvitationStatus.INVITED)
                .createdAt(LocalDateTime.of(2026, 8, 18, 10, 0))
                .build();
        when(invitationService.getSentInvitations(1L, 10L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/projects/10/invitations")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].invitationId").value(100))
                .andExpect(jsonPath("$.data[0].projectId").value(10))
                .andExpect(jsonPath("$.data[0].projectName").value("Wrap"))
                .andExpect(jsonPath("$.data[0].inviteeMemberId").value(2))
                .andExpect(jsonPath("$.data[0].inviteeEmail")
                        .value("invitee@example.com"))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[0].status").value("INVITED"))
                .andExpect(jsonPath("$.data[0].createdAt")
                        .value("2026-08-18T10:00:00"))
                .andExpect(jsonPath("$.message")
                        .value("Sent project invitations retrieved."));

        verify(invitationService).getSentInvitations(1L, 10L);
    }

    @Test
    void getSentInvitationsReturnsEmptyList() throws Exception {
        when(invitationService.getSentInvitations(1L, 10L)).thenReturn(List.of());

        mockMvc.perform(get("/projects/10/invitations")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(invitationService).getSentInvitations(1L, 10L);
    }

    @Test
    void getSentInvitationsWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/projects/10/invitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(invitationService);
    }

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

    @Test
    void acceptReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        InvitationResponse response = InvitationResponse.builder()
                .invitationId(100L)
                .projectId(10L)
                .projectName("Wrap")
                .inviteeMemberId(2L)
                .inviteeEmail("invitee@example.com")
                .role(ProjectMemberRole.MEMBER)
                .status(InvitationStatus.ACCEPTED)
                .createdAt(LocalDateTime.of(2026, 8, 22, 10, 0))
                .build();
        when(invitationService.accept(2L, 100L)).thenReturn(response);

        mockMvc.perform(patch("/invitations/100/accept")
                        .with(user(memberDetails(2L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invitationId").value(100))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.projectName").value("Wrap"))
                .andExpect(jsonPath("$.data.inviteeMemberId").value(2))
                .andExpect(jsonPath("$.data.inviteeEmail")
                        .value("invitee@example.com"))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.message")
                        .value("Project invitation accepted."));

        verify(invitationService).accept(2L, 100L);
    }

    @Test
    void acceptWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/invitations/100/accept")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void rejectReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        InvitationResponse response = InvitationResponse.builder()
                .invitationId(100L)
                .projectId(10L)
                .projectName("Wrap")
                .inviteeMemberId(2L)
                .inviteeEmail("invitee@example.com")
                .role(ProjectMemberRole.MEMBER)
                .status(InvitationStatus.REJECTED)
                .createdAt(LocalDateTime.of(2026, 8, 22, 10, 0))
                .build();
        when(invitationService.reject(2L, 100L)).thenReturn(response);

        mockMvc.perform(patch("/invitations/100/reject")
                        .with(user(memberDetails(2L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invitationId").value(100))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.message")
                        .value("Project invitation rejected."));

        verify(invitationService).reject(2L, 100L);
    }

    @Test
    void rejectWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/invitations/100/reject")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(invitationService);
    }

    @Test
    void cancelReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        mockMvc.perform(delete("/projects/10/invitations/100")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message")
                        .value("Project invitation canceled."));

        verify(invitationService).cancel(1L, 10L, 100L);
    }

    @Test
    void cancelWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/projects/10/invitations/100")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

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
