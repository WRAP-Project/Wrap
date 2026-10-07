package com.wrap.domain.task.service;

import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.dto.TaskCreateRequest;
import com.wrap.domain.task.dto.TaskResponse;
import com.wrap.domain.task.dto.TaskStatusUpdateRequest;
import com.wrap.domain.task.dto.TaskUpdateRequest;
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
    private final MilestoneRepository milestoneRepository;
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
    public TaskResponse create(Long memberId, Long projectId, TaskCreateRequest request) {
        Project project = projectMemberValidator.findJoinedMember(memberId, projectId).getProject();

        Task task = Task.create(
                project,
                findMilestone(projectId, request.milestoneId()),
                findAssignee(projectId, request.assigneeId()),
                request.title(),
                request.description(),
                request.dueDate(),
                request.priority(),
                Boolean.TRUE.equals(request.deliverable())
        );

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(
            Long memberId,
            Long projectId,
            Long taskId,
            TaskUpdateRequest request
    ) {
        ProjectMember requester = projectMemberValidator.findJoinedMember(memberId, projectId);
        Task task = findProjectTask(projectId, taskId);
        validateWritable(requester, task);

        task.update(
                request.milestoneId() == null
                        ? task.getMilestone()
                        : findMilestone(projectId, request.milestoneId()),
                request.assigneeId() == null
                        ? task.getAssignee()
                        : findAssignee(projectId, request.assigneeId()),
                request.title() == null ? task.getTitle() : request.title(),
                request.description() == null ? task.getDescription() : request.description(),
                request.dueDate() == null ? task.getDueDate() : request.dueDate(),
                request.priority() == null ? task.getPriority() : request.priority(),
                request.deliverable() == null ? task.isDeliverable() : request.deliverable()
        );

        return TaskResponse.from(task);
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
        validateWritable(requester, task);

        taskRepository.delete(task);
    }

    private Task findProjectTask(Long projectId, Long taskId) {
        return taskRepository.findByIdAndProjectIdWithAssignee(taskId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
    }

    private void validateWritable(ProjectMember requester, Task task) {
        if (!requester.isOwner() && !task.isAssignedTo(requester.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private Milestone findMilestone(Long projectId, Long milestoneId) {
        if (milestoneId == null) {
            return null;
        }

        return milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
    }

    private ProjectMember findAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }

        return projectMemberValidator.findJoinedProjectMember(projectId, assigneeId);
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
