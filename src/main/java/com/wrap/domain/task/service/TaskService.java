package com.wrap.domain.task.service;

import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.dto.TaskResponse;
import com.wrap.domain.task.dto.TaskStatusUpdateRequest;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectMemberValidator projectMemberValidator;

    public List<TaskResponse> findTasks(
            Long memberId,
            Long projectId,
            Long milestoneId,
            TaskStatus status,
            Long assigneeId,
            Boolean deliverable,
            LocalDate dueFrom,
            LocalDate dueTo,
            String sort
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        validateDateRange(dueFrom, dueTo);

        return taskRepository.findProjectTasks(
                        projectId,
                        milestoneId,
                        status,
                        assigneeId,
                        deliverable,
                        dueFrom,
                        dueTo
                )
                .stream()
                .sorted(comparatorOf(sort))
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse updateStatus(
            Long memberId,
            Long projectId,
            Long taskId,
            TaskStatusUpdateRequest request
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        Task task = findProjectTask(projectId, taskId);
        task.updateStatus(request.status());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long memberId, Long projectId, Long taskId) {
        ProjectMember requester = projectMemberValidator.findJoinedMember(memberId, projectId);
        Task task = findProjectTask(projectId, taskId);

        if (!requester.isOwner() && !task.isAssignedTo(requester.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        taskRepository.delete(task);
    }

    private Task findProjectTask(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProjectIdWithAssignee(taskId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
    }

    private void validateDateRange(LocalDate dueFrom, LocalDate dueTo) {
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private Comparator<Task> comparatorOf(String sort) {
        if ("recent".equalsIgnoreCase(sort)) {
            return Comparator.comparing(
                    Task::getUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }
        if ("status".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Task::getStatus)
                    .thenComparing(Task::getId);
        }

        return Comparator.comparing(
                        Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(Task::getId);
    }
}
