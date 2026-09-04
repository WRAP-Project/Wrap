package com.wrap.domain.calendar.service;

import com.wrap.domain.calendar.dto.CalendarRiskCheckResponse;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
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

    public List<CalendarRiskCheckResponse> findRiskChecks(
            Long memberId,
            Long projectId,
            TaskStatus status,
            Long assigneeProjectMemberId,
            LocalDate dueFrom,
            LocalDate dueTo
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        validateDateRange(dueFrom, dueTo);
        LocalDate today = LocalDate.now();

        return taskRepository.findCalendarRiskChecks(
                        projectId,
                        status,
                        assigneeProjectMemberId,
                        dueFrom,
                        dueTo,
                        TaskStatus.DONE
                )
                .stream()
                .map(task -> CalendarRiskCheckResponse.from(task, today))
                .toList();
    }

    private void validateDateRange(LocalDate dueFrom, LocalDate dueTo) {
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
