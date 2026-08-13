package com.wrap.domain.schedule.service;

import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.schedule.dto.RiskCheckResponse;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final TaskRepository taskRepository;
    private final ProjectMemberValidator projectMemberValidator;

    public List<RiskCheckResponse> findRiskChecks(
            Long memberId,
            Long projectId,
            TaskStatus status,
            Long assigneeProjectMemberId,
            LocalDate dueFrom,
            LocalDate dueTo
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        if (assigneeProjectMemberId != null) {
            projectMemberValidator.findJoinedProjectMember(projectId, assigneeProjectMemberId);
        }
        validateDateRange(dueFrom, dueTo);

        LocalDate today = LocalDate.now();
        return taskRepository.findRiskChecks(projectId, status, assigneeProjectMemberId, dueFrom, dueTo)
                .stream()
                .map(task -> RiskCheckResponse.from(task, today))
                .toList();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
