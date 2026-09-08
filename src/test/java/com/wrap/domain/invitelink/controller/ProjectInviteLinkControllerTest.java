package com.wrap.domain.invitelink.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkInfoResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteJoinResponse;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkSummaryResponse;
import com.wrap.domain.invitelink.service.ProjectInviteLinkService;
import com.wrap.domain.member.entity.Member;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectInviteLinkController.class)
@Import(SecurityConfig.class)
class ProjectInviteLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectInviteLinkService inviteLinkService;

    @Test
    void createReturnsCreatedAndUsesAuthenticatedMemberId() throws Exception {
        ProjectInviteLinkResponse response = ProjectInviteLinkResponse.builder()
                .inviteLinkId(100L)
                .projectId(10L)
                .projectName("Wrap")
                .createdByMemberId(1L)
                .inviteUrl("https://wrap-client.vercel.app/join/raw-token")
                .active(true)
                .createdAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .build();
        when(inviteLinkService.create(1L, 10L)).thenReturn(response);

        mockMvc.perform(post("/projects/10/invite-links")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inviteLinkId").value(100))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.projectName").value("Wrap"))
                .andExpect(jsonPath("$.data.createdByMemberId").value(1))
                .andExpect(jsonPath("$.data.inviteUrl")
                        .value("https://wrap-client.vercel.app/join/raw-token"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.message").value("Project invite link created."));

        verify(inviteLinkService).create(1L, 10L);
    }

    @Test
    void createWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/projects/10/invite-links").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(inviteLinkService);
    }

    @Test
    void getInviteLinksReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        ProjectInviteLinkSummaryResponse response = ProjectInviteLinkSummaryResponse.builder()
                .inviteLinkId(100L)
                .projectId(10L)
                .createdByMemberId(1L)
                .createdByNickname("owner")
                .active(true)
                .createdAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .build();
        when(inviteLinkService.getInviteLinks(1L, 10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/projects/10/invite-links")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].inviteLinkId").value(100))
                .andExpect(jsonPath("$.data[0].projectId").value(10))
                .andExpect(jsonPath("$.data[0].createdByMemberId").value(1))
                .andExpect(jsonPath("$.data[0].createdByNickname").value("owner"))
                .andExpect(jsonPath("$.data[0].active").value(true))
                .andExpect(jsonPath("$.data[0].inviteUrl").doesNotExist())
                .andExpect(jsonPath("$.message")
                        .value("Project invite links retrieved."));

        verify(inviteLinkService).getInviteLinks(1L, 10L);
    }

    @Test
    void getInviteLinksWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/projects/10/invite-links"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(inviteLinkService);
    }

    @Test
    void revokeReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        mockMvc.perform(delete("/projects/10/invite-links/100")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Project invite link revoked."));

        verify(inviteLinkService).revoke(1L, 10L, 100L);
    }

    @Test
    void revokeWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/projects/10/invite-links/100").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(inviteLinkService);
    }

    @Test
    void getInviteLinkInfoIsPublicAndReturnsProjectInformation() throws Exception {
        ProjectInviteLinkInfoResponse response = ProjectInviteLinkInfoResponse.builder()
                .projectId(10L)
                .projectName("Wrap")
                .projectColor("#CDEA6F")
                .inviterNickname("owner")
                .build();
        when(inviteLinkService.getInviteLinkInfo("raw-token")).thenReturn(response);

        mockMvc.perform(get("/invite-links/raw-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.projectName").value("Wrap"))
                .andExpect(jsonPath("$.data.projectColor").value("#CDEA6F"))
                .andExpect(jsonPath("$.data.inviterNickname").value("owner"))
                .andExpect(jsonPath("$.message")
                        .value("Project invite link information retrieved."));

        verify(inviteLinkService).getInviteLinkInfo("raw-token");
    }

    @Test
    void joinReturnsOkAndUsesAuthenticatedMemberId() throws Exception {
        ProjectInviteJoinResponse response = ProjectInviteJoinResponse.builder()
                .projectId(10L)
                .projectName("Wrap")
                .projectMemberId(200L)
                .memberId(2L)
                .role(ProjectMemberRole.MEMBER)
                .status(ProjectMemberStatus.JOINED)
                .joinedAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .build();
        when(inviteLinkService.join(2L, "raw-token")).thenReturn(response);

        mockMvc.perform(post("/invite-links/raw-token/join")
                        .with(user(memberDetails(2L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").value(10))
                .andExpect(jsonPath("$.data.projectName").value("Wrap"))
                .andExpect(jsonPath("$.data.projectMemberId").value(200))
                .andExpect(jsonPath("$.data.memberId").value(2))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.status").value("JOINED"))
                .andExpect(jsonPath("$.message")
                        .value("Joined project through invite link."));

        verify(inviteLinkService).join(2L, "raw-token");
    }

    @Test
    void joinWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/invite-links/raw-token/join").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(inviteLinkService);
    }

    @Test
    void createReturnsConflictWhenActiveInviteLinkAlreadyExists() throws Exception {
        when(inviteLinkService.create(1L, 10L))
                .thenThrow(new CustomException(ErrorCode.INVITE_LINK_ALREADY_EXISTS));

        mockMvc.perform(post("/projects/10/invite-links")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code")
                        .value("INVITE_LINK_ALREADY_EXISTS"));
    }

    @Test
    void getInviteLinkInfoReturnsNotFoundForInvalidToken() throws Exception {
        when(inviteLinkService.getInviteLinkInfo("invalid-token"))
                .thenThrow(new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND));

        mockMvc.perform(get("/invite-links/invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVITE_LINK_NOT_FOUND"));
    }

    @Test
    void joinReturnsConflictWhenMemberAlreadyJoined() throws Exception {
        when(inviteLinkService.join(2L, "raw-token"))
                .thenThrow(new CustomException(ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS));

        mockMvc.perform(post("/invite-links/raw-token/join")
                        .with(user(memberDetails(2L)))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code")
                        .value("PROJECT_MEMBER_ALREADY_EXISTS"));
    }

    @Test
    void revokeReturnsNotFoundWhenInviteLinkDoesNotExist() throws Exception {
        doThrow(new CustomException(ErrorCode.INVITE_LINK_NOT_FOUND))
                .when(inviteLinkService)
                .revoke(1L, 10L, 100L);

        mockMvc.perform(delete("/projects/10/invite-links/100")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVITE_LINK_NOT_FOUND"));
    }

    private MemberDetails memberDetails(Long memberId) {
        Member member = Member.builder()
                .email("member@example.com")
                .password("password1234")
                .nickname("member")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return new MemberDetails(member);
    }
}
