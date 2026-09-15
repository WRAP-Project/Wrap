package com.wrap.domain.task.controller;

import com.wrap.domain.task.dto.TaskResponse;
import com.wrap.domain.task.dto.TaskStatusUpdateRequest;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.service.TaskService;
import com.wrap.global.common.ApiResponse;
import com.wrap.global.security.MemberDetails;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/projects/{projectId}/tasks")
    public ApiResponse<List<TaskResponse>> findTasks(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @RequestParam(required = false) Long milestoneId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean deliverable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.success(
                taskService.findTasks(
                        memberDetails.getMemberId(),
                        projectId,
                        milestoneId,
                        status,
                        assigneeId,
                        deliverable,
                        dueFrom,
                        dueTo,
                        sort
                ),
                "Tasks retrieved."
        );
    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}/status")
    public ApiResponse<TaskResponse> updateStatus(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody TaskStatusUpdateRequest request
    ) {
        return ApiResponse.success(
                taskService.updateStatus(memberDetails.getMemberId(), projectId, taskId, request),
                "Task status updated."
        );
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @PathVariable Long projectId,
            @PathVariable Long taskId
    ) {
        taskService.delete(memberDetails.getMemberId(), projectId, taskId);
        return ApiResponse.success("Task deleted.");
    }
}
