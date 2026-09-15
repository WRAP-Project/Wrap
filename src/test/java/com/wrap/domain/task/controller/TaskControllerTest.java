package com.wrap.domain.task.controller;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.task.dto.TaskResponse;
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.service.TaskService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void findTasksReturnsTasksAndUsesFilters() throws Exception {
        when(taskService.findTasks(
                eq(1L),
                eq(10L),
                eq(20L),
                eq(TaskStatus.IN_PROGRESS),
                eq(30L),
                eq(true),
                eq(LocalDate.of(2026, 9, 1)),
                eq(LocalDate.of(2026, 9, 30)),
                eq("dueDate")
        )).thenReturn(List.of(new TaskResponse(
                1L,
                10L,
                20L,
                null,
                "Task",
                "Task description",
                TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 9, 30),
                0,
                TaskPriority.MEDIUM,
                true
        )));

        mockMvc.perform(get("/projects/10/tasks")
                        .with(user(memberDetails(1L)))
                        .param("milestoneId", "20")
                        .param("status", "IN_PROGRESS")
                        .param("assigneeId", "30")
                        .param("deliverable", "true")
                        .param("dueFrom", "2026-09-01")
                        .param("dueTo", "2026-09-30")
                        .param("sort", "dueDate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("Tasks retrieved."));
    }

    @Test
    void updateStatusReturnsUpdatedTask() throws Exception {
        when(taskService.updateStatus(eq(1L), eq(10L), eq(2L), any()))
                .thenReturn(new TaskResponse(
                        2L,
                        10L,
                        null,
                        null,
                        "Task",
                        null,
                        TaskStatus.DONE,
                        null,
                        0,
                        TaskPriority.MEDIUM,
                        false
                ));

        mockMvc.perform(patch("/projects/10/tasks/2/status")
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
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.message").value("Task status updated."));

        verify(taskService).updateStatus(eq(1L), eq(10L), eq(2L), any());
    }

    @Test
    void updateStatusWithoutStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/projects/10/tasks/2/status")
                        .with(user(memberDetails(1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(taskService);
    }

    @Test
    void deleteReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/projects/10/tasks/2")
                        .with(user(memberDetails(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Task deleted."));

        verify(taskService).delete(1L, 10L, 2L);
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
