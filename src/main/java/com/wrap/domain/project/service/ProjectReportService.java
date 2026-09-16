package com.wrap.domain.project.service;

import com.wrap.domain.project.dto.response.ProjectReportAreaResponse;
import com.wrap.domain.project.dto.response.ProjectReportResponse;
import com.wrap.domain.project.dto.response.ProjectReportRiskResponse;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectReportService {

    private static final int DUE_SOON_DAYS = 3;

    private final ProjectMemberValidator projectMemberValidator;
    private final TaskRepository taskRepository;

    public ProjectReportResponse getReport(Long memberId, Long projectId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        List<Task> tasks = taskRepository.findProjectReportTasks(projectId);
        long total = tasks.size();
        long doneCount = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
        long needsCheckCount = tasks.stream()
                .filter(this::needsCheck)
                .count();
        long inProgressCount = Math.max(0, total - doneCount - needsCheckCount);
        int percent = total == 0 ? 0 : (int) Math.round(doneCount * 100.0 / total);

        return new ProjectReportResponse(
                percent,
                doneCount,
                inProgressCount,
                needsCheckCount,
                buildAreas(tasks),
                buildRisks(tasks)
        );
    }

    private List<ProjectReportAreaResponse> buildAreas(List<Task> tasks) {
        Map<String, List<Task>> byArea = tasks.stream()
                .collect(Collectors.groupingBy(areaOf(), java.util.LinkedHashMap::new, Collectors.toList()));

        return byArea.entrySet().stream()
                .map(entry -> {
                    List<Task> areaTasks = entry.getValue();
                    long done = areaTasks.stream()
                            .filter(task -> task.getStatus() == TaskStatus.DONE)
                            .count();
                    boolean delayed = areaTasks.stream().anyMatch(this::needsCheck);
                    int percent = areaTasks.isEmpty()
                            ? 0
                            : (int) Math.round(done * 100.0 / areaTasks.size());
                    return new ProjectReportAreaResponse(
                            entry.getKey(),
                            percent,
                            delayed,
                            delayed ? "확인 필요" : null
                    );
                })
                .sorted(Comparator.comparing(ProjectReportAreaResponse::area))
                .toList();
    }

    private List<ProjectReportRiskResponse> buildRisks(List<Task> tasks) {
        LocalDate today = LocalDate.now();
        return tasks.stream()
                .filter(this::needsCheck)
                .sorted(Comparator.comparing(
                        Task::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .limit(5)
                .map(task -> new ProjectReportRiskResponse(
                        task.getId(),
                        task.getTitle(),
                        riskDetail(task, today)
                ))
                .toList();
    }

    private Function<Task, String> areaOf() {
        return task -> {
            ProjectMember assignee = task.getAssignee();
            return assignee == null ? "UNASSIGNED" : assignee.getRole().name();
        };
    }

    private boolean needsCheck(Task task) {
        if (task.getStatus() == TaskStatus.HOLD) {
            return true;
        }
        LocalDate dueDate = task.getDueDate();
        return task.getStatus() != TaskStatus.DONE
                && dueDate != null
                && !dueDate.isAfter(LocalDate.now().plusDays(DUE_SOON_DAYS));
    }

    private String riskDetail(Task task, LocalDate today) {
        if (task.getStatus() == TaskStatus.HOLD) {
            return "진행이 막힌 할 일입니다.";
        }
        if (task.getDueDate() != null && task.getDueDate().isBefore(today)) {
            return "마감일이 지난 할 일입니다.";
        }
        return "마감이 가까운 할 일입니다.";
    }
}
