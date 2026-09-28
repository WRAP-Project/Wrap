package com.wrap.domain.milestone.service;

import com.wrap.domain.milestone.dto.request.MilestoneCreateRequest;
import com.wrap.domain.milestone.dto.request.MilestoneUpdateRequest;
import com.wrap.domain.milestone.dto.response.MilestoneResponse;
import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
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

    public List<MilestoneResponse> findProjectMilestones(Long memberId, Long projectId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);

        return milestoneRepository.findAllByProjectIdOrderByDueDateAscIdAsc(projectId)
                .stream()
                .map(MilestoneResponse::from)
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

        return MilestoneResponse.from(milestone);
    }

    @Transactional
    public void delete(Long memberId, Long projectId, Long milestoneId) {
        projectMemberValidator.findJoinedMember(memberId, projectId);
        Milestone milestone = findProjectMilestone(projectId, milestoneId);

        taskRepository.clearMilestone(milestoneId);
        milestoneRepository.delete(milestone);
    }

    private Milestone findProjectMilestone(Long projectId, Long milestoneId) {
        return milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
    }
}
