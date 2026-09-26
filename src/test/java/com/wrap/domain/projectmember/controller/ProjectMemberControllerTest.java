package com.wrap.domain.projectmember.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.projectmember.dto.request.ProjectMemberRoleUpdateRequest;
import com.wrap.domain.projectmember.dto.request.ProjectMemberWorkRoleUpdateRequest;
import com.wrap.domain.projectmember.dto.response.ProjectMemberResponse;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.service.ProjectMemberService;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
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

@WebMvcTest(ProjectMemberController.class)
@Import(SecurityConfig.class)
class ProjectMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectMemberService projectMemberService;

    @Test
    void getProjectMembersReturnsJoinedMembersAndUsesAuthenticatedMemberId()
            throws Exception {
        List<ProjectMemberResponse> responses = List.of(
                ProjectMemberResponse.builder()
                        .projectMemberId(100L)
                        .memberId(1L)
                        .nickname("owner")
                        .profileImage("owner.png")
                        .role(ProjectMemberRole.OWNER)
                        .workRole("PM")
                        .status(ProjectMemberStatus.JOINED)
                        .joinedAt(LocalDateTime.of(2026, 7, 1, 9, 0))
                        .build(),
                ProjectMemberResponse.builder()
                        .projectMemberId(200L)
                        .memberId(2L)
                        .nickname("member")
                        .role(ProjectMemberRole.MEMBER)
                        .workRole("프론트엔드")
                        .status(ProjectMemberStatus.JOINED)
                        .joinedAt(LocalDateTime.of(2026, 7, 2, 9, 0))
                        .build()
        );
        when(projectMemberService.getProjectMembers(1L, 10L))
                .thenReturn(responses);

        mockMvc.perform(get("/projects/10/members")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].projectMemberId").value(100))
                .andExpect(jsonPath("$.data[0].memberId").value(1))
                .andExpect(jsonPath("$.data[0].nickname").value("owner"))
                .andExpect(jsonPath("$.data[0].profileImage").value("owner.png"))
                .andExpect(jsonPath("$.data[0].role").value("OWNER"))
                .andExpect(jsonPath("$.data[0].workRole").value("PM"))
                .andExpect(jsonPath("$.data[0].status").value("JOINED"))
                .andExpect(jsonPath("$.data[1].projectMemberId").value(200))
                .andExpect(jsonPath("$.data[1].nickname").value("member"))
                .andExpect(jsonPath("$.data[1].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[1].workRole").value("프론트엔드"))
                .andExpect(jsonPath("$.message").value("Project members retrieved."));

        verify(projectMemberService).getProjectMembers(1L, 10L);
    }

    @Test
    void getProjectMembersWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/projects/10/members"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeRoleReturnsUpdatedMemberAndUsesAuthenticatedMemberId() throws Exception {
        ProjectMemberResponse response = ProjectMemberResponse.builder()
                .projectMemberId(200L)
                .memberId(2L)
                .nickname("member")
                .role(ProjectMemberRole.OWNER)
                .status(ProjectMemberStatus.JOINED)
                .joinedAt(LocalDateTime.of(2026, 7, 2, 9, 0))
                .build();
        when(projectMemberService.changeRole(
                eq(1L),
                eq(10L),
                eq(200L),
                any(ProjectMemberRoleUpdateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/projects/10/members/200/role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "OWNER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectMemberId").value(200))
                .andExpect(jsonPath("$.data.memberId").value(2))
                .andExpect(jsonPath("$.data.nickname").value("member"))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.status").value("JOINED"))
                .andExpect(jsonPath("$.message").value("Project member role updated."));

        verify(projectMemberService).changeRole(
                eq(1L),
                eq(10L),
                eq(200L),
                any(ProjectMemberRoleUpdateRequest.class)
        );
    }

    @Test
    void changeRoleWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/projects/10/members/200/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "OWNER"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeRoleWithoutRoleReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/projects/10/members/200/role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("role"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeWorkRoleReturnsUpdatedMemberAndUsesAuthenticatedMemberId()
            throws Exception {
        ProjectMemberResponse response = ProjectMemberResponse.builder()
                .projectMemberId(200L)
                .memberId(2L)
                .nickname("member")
                .role(ProjectMemberRole.MEMBER)
                .workRole("프론트엔드")
                .status(ProjectMemberStatus.JOINED)
                .joinedAt(LocalDateTime.of(2026, 7, 2, 9, 0))
                .build();
        when(projectMemberService.changeWorkRole(
                eq(1L),
                eq(10L),
                eq(200L),
                any(ProjectMemberWorkRoleUpdateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(patch("/projects/10/members/200/work-role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workRole": "프론트엔드"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectMemberId").value(200))
                .andExpect(jsonPath("$.data.memberId").value(2))
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.workRole").value("프론트엔드"))
                .andExpect(jsonPath("$.data.status").value("JOINED"))
                .andExpect(jsonPath("$.message")
                        .value("Project member work role updated."));

        verify(projectMemberService).changeWorkRole(
                eq(1L),
                eq(10L),
                eq(200L),
                any(ProjectMemberWorkRoleUpdateRequest.class)
        );
    }

    @Test
    void changeWorkRoleWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/projects/10/members/200/work-role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRole\":\"QA\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeWorkRoleWithoutFieldReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/projects/10/members/200/work-role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("workRole"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeWorkRoleWithNullReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/projects/10/members/200/work-role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRole\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("workRole"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeWorkRoleLongerThanFiftyCharactersReturnsBadRequest() throws Exception {
        String workRole = "a".repeat(51);

        mockMvc.perform(patch("/projects/10/members/200/work-role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRole\":\"" + workRole + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("workRole"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void changeWorkRoleInCompletedProjectReturnsConflict() throws Exception {
        when(projectMemberService.changeWorkRole(
                eq(1L),
                eq(10L),
                eq(200L),
                any(ProjectMemberWorkRoleUpdateRequest.class)
        )).thenThrow(new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED));

        mockMvc.perform(patch("/projects/10/members/200/work-role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workRole\":\"QA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code")
                        .value("PROJECT_ALREADY_COMPLETED"));
    }

    @Test
    void leaveProjectReturnsSuccessAndUsesAuthenticatedMemberId() throws Exception {
        mockMvc.perform(delete("/projects/10/members/me")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("Project left."));

        verify(projectMemberService).leaveProject(1L, 10L);
    }

    @Test
    void leaveProjectWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/projects/10/members/me")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void removeMemberReturnsSuccessAndUsesAuthenticatedMemberId() throws Exception {
        mockMvc.perform(delete("/projects/10/members/200")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("Project member removed."));

        verify(projectMemberService).removeMember(1L, 10L, 200L);
    }

    @Test
    void removeMemberWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/projects/10/members/200")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectMemberService);
    }

    @Test
    void demotingLastOwnerReturnsConflict() throws Exception {
        when(projectMemberService.changeRole(
                eq(1L), eq(10L), eq(100L), any(ProjectMemberRoleUpdateRequest.class)
        )).thenThrow(new CustomException(ErrorCode.LAST_PROJECT_OWNER));

        mockMvc.perform(patch("/projects/10/members/100/role")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LAST_PROJECT_OWNER"));
    }

    @Test
    void lastOwnerLeavingReturnsConflict() throws Exception {
        doThrow(new CustomException(ErrorCode.LAST_PROJECT_OWNER))
                .when(projectMemberService).leaveProject(1L, 10L);

        mockMvc.perform(delete("/projects/10/members/me")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LAST_PROJECT_OWNER"));
    }

    @Test
    void removingOwnerReturnsConflict() throws Exception {
        doThrow(new CustomException(ErrorCode.PROJECT_OWNER_CANNOT_BE_REMOVED))
                .when(projectMemberService).removeMember(1L, 10L, 200L);

        mockMvc.perform(delete("/projects/10/members/200")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PROJECT_OWNER_CANNOT_BE_REMOVED"));
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
