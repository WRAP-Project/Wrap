package com.wrap.domain.project.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.request.ProjectUpdateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.dto.response.ProjectSummaryResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public ProjectResponse create(Long memberId, ProjectCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        validateDateRange(request.getStartDate(), request.getEndDate());

        Project project = Project.create(
                request.getName(),
                request.getDescription(),
                request.getGoal(),
                request.getSuccessCriteria(),
                request.getStartDate(),
                request.getEndDate()
        );
        Project savedProject = projectRepository.save(project);

        ProjectMember owner = ProjectMember.createOwner(
                member,
                savedProject,
                LocalDateTime.now()
        );
        projectMemberRepository.save(owner);

        return ProjectResponse.from(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getMyProjects(Long memberId) {
        return projectMemberRepository
                .findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
                        memberId,
                        ProjectMemberStatus.JOINED
                )
                .stream()
                .map(ProjectMember::getProject)
                .map(ProjectSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long memberId, Long projectId) {
        Project project = findActiveProject(projectId);
        findJoinedMember(memberId, projectId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(
            Long memberId,
            Long projectId,
            ProjectUpdateRequest request
    ) {
        Project project = findActiveProject(projectId);
        ProjectMember projectMember = findJoinedMember(memberId, projectId);
        validateOwner(projectMember);
        validateDateRange(request.getStartDate(), request.getEndDate());

        project.update(
                request.getName(),
                request.getDescription(),
                request.getGoal(),
                request.getSuccessCriteria(),
                request.getStartDate(),
                request.getEndDate()
        );

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse complete(Long memberId, Long projectId) {
        Project project = findActiveProject(projectId);
        ProjectMember projectMember = findJoinedMember(memberId, projectId);
        validateOwner(projectMember);

        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        project.complete(LocalDateTime.now());
        return ProjectResponse.from(project);
    }

    private Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectMember findJoinedMember(Long memberId, Long projectId) {
        return projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                        memberId,
                        projectId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_ACCESS_DENIED));
    }

    private void validateOwner(ProjectMember projectMember) {
        if (!projectMember.isOwner()) {
            throw new CustomException(ErrorCode.PROJECT_OWNER_REQUIRED);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
