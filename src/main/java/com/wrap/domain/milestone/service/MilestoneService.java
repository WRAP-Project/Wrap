package com.wrap.domain.milestone.service;

import com.wrap.domain.milestone.dto.request.MilestoneCreateRequest;
import com.wrap.domain.milestone.dto.request.MilestoneUpdateRequest;
import com.wrap.domain.milestone.dto.response.MilestoneResponse;
import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.enums.MilestoneStatus;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberValidator projectMemberValidator;

    @Transactional
    public List<MilestoneResponse> findProjectMilestones(Long memberId, Long projectId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);

        return milestoneRepository.findAllByProjectIdOrderByDueDateAscIdAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MilestoneResponse create(Long memberId, Long projectId, MilestoneCreateRequest request) {
        Project project = projectMemberValidator.findJoinedMember(memberId, projectId).getProject();

        Milestone milestone = Milestone.create(
                project,
                request.title(),
                request.description(),
                request.dueDate()
        );

        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneResponse update(
            Long memberId,
            Long projectId,
            Long milestoneId,
            MilestoneUpdateRequest request
    ) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        Milestone milestone = findProjectMilestone(projectId, milestoneId);

        milestone.update(
                request.title() == null ? milestone.getTitle() : request.title(),
                request.description() == null ? milestone.getDescription() : request.description(),
                request.dueDate() == null ? milestone.getDueDate() : request.dueDate()
        );

        return toResponse(milestone);
    }

    @Transactional
    public void delete(Long memberId, Long projectId, Long milestoneId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        Milestone milestone = findProjectMilestone(projectId, milestoneId);

        taskRepository.clearMilestone(milestoneId);
        milestoneRepository.delete(milestone);
    }

    private MilestoneResponse toResponse(Milestone milestone) {
        List<Task> tasks = taskRepository.findByMilestoneId(milestone.getId());
        int totalTaskCount = tasks.size();
        int doneTaskCount = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        promoteIfAllTasksDone(milestone, totalTaskCount, doneTaskCount);

        return MilestoneResponse.of(milestone, totalTaskCount, doneTaskCount);
    }

    /**
     * 연결된 할 일이 모두 완료되면 마일스톤을 완료로 승격시킨다.
     * 승격만 수행하고, 사용자가 완료로 표시한 마일스톤을 다시 진행중으로 되돌리지는 않는다.
     */
    private void promoteIfAllTasksDone(Milestone milestone, int totalTaskCount, int doneTaskCount) {
        if (milestone.getStatus() == MilestoneStatus.IN_PROGRESS
                && totalTaskCount > 0
                && doneTaskCount == totalTaskCount) {
            milestone.updateStatus(MilestoneStatus.DONE);
        }
    }

    private Milestone findProjectMilestone(Long projectId, Long milestoneId) {
        return milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
    }
}
