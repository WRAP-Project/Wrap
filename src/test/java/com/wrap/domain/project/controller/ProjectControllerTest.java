package com.wrap.domain.project.controller;

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

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.project.service.ProjectService;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void createReturnsCreatedAndUsesAuthenticatedMemberId() throws Exception {
        ProjectResponse response = ProjectResponse.builder()
                .id(10L)
                .name("Wrap")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status(ProjectStatus.IN_PROGRESS)
                .build();
        when(projectService.create(eq(1L), any(ProjectCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/projects")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Wrap"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("Project created."));

        verify(projectService).create(eq(1L), any(ProjectCreateRequest.class));
    }

    @Test
    void createWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/projects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectService);
    }

    @Test
    void createWithBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/projects")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "description": "Project description",
                                  "startDate": "2026-07-01",
                                  "endDate": "2026-08-31"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("name"));

        verifyNoInteractions(projectService);
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

    private String validCreateRequest() {
        return """
                {
                  "name": "Wrap",
                  "description": "Project description",
                  "goal": "Project goal",
                  "successCriteria": "Success criteria",
                  "startDate": "2026-07-01",
                  "endDate": "2026-08-31"
                }
                """;
    }
}
