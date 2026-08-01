package com.wrap.domain.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.dto.response.ProjectSummaryResponse;
import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.project.service.ProjectService;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    void getMyProjectsReturnsSummariesAndUsesAuthenticatedMemberId() throws Exception {
        List<ProjectSummaryResponse> responses = List.of(
                ProjectSummaryResponse.builder()
                        .id(10L)
                        .name("Wrap")
                        .status(ProjectStatus.IN_PROGRESS)
                        .startDate(LocalDate.of(2026, 7, 1))
                        .endDate(LocalDate.of(2026, 8, 31))
                        .build(),
                ProjectSummaryResponse.builder()
                        .id(20L)
                        .name("Graduation")
                        .status(ProjectStatus.COMPLETED)
                        .startDate(LocalDate.of(2026, 3, 1))
                        .endDate(LocalDate.of(2026, 6, 30))
                        .build()
        );
        when(projectService.getMyProjects(1L)).thenReturn(responses);

        mockMvc.perform(get("/projects")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Wrap"))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data[1].id").value(20))
                .andExpect(jsonPath("$.data[1].name").value("Graduation"))
                .andExpect(jsonPath("$.data[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$.message").value("My projects retrieved."));

        verify(projectService).getMyProjects(1L);
    }

    @Test
    void getMyProjectsWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        verifyNoInteractions(projectService);
    }

    @Test
    void getProjectReturnsDetailAndUsesAuthenticatedMemberId() throws Exception {
        ProjectResponse response = ProjectResponse.builder()
                .id(10L)
                .name("Wrap")
                .description("Project description")
                .goal("Project goal")
                .successCriteria("Success criteria")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .status(ProjectStatus.IN_PROGRESS)
                .build();
        when(projectService.getProject(1L, 10L)).thenReturn(response);

        mockMvc.perform(get("/projects/10")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Wrap"))
                .andExpect(jsonPath("$.data.description").value("Project description"))
                .andExpect(jsonPath("$.data.goal").value("Project goal"))
                .andExpect(jsonPath("$.data.successCriteria").value("Success criteria"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("Project retrieved."));

        verify(projectService).getProject(1L, 10L);
    }

    @Test
    void getProjectWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/projects/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

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
