package com.wrap.domain.project.controller;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.dto.response.ProjectReportResponse;
import com.wrap.domain.project.service.ProjectReportService;
import com.wrap.global.security.MemberDetails;
import com.wrap.global.security.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectReportController.class)
@Import(SecurityConfig.class)
class ProjectReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectReportService projectReportService;

    @Test
    void getReportReturnsProjectReport() throws Exception {
        when(projectReportService.getReport(1L, 10L))
                .thenReturn(new ProjectReportResponse(67, 6, 3, 2, List.of(), List.of()));

        mockMvc.perform(get("/projects/10/report")
                        .with(user(memberDetails(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.percent").value(67))
                .andExpect(jsonPath("$.data.doneCount").value(6))
                .andExpect(jsonPath("$.message").value("Project report retrieved."));

        verify(projectReportService).getReport(1L, 10L);
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
