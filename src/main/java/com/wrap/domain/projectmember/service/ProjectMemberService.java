package com.wrap.domain.projectmember.service;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.dto.request.ProjectMemberRoleUpdateRequest;
import com.wrap.domain.projectmember.dto.response.ProjectMemberResponse;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Long memberId, Long projectId) {
        findActiveProject(projectId);
        findJoinedMember(memberId, projectId);

        return projectMemberRepository
                .findAllByProjectIdAndStatus(projectId, ProjectMemberStatus.JOINED)
                .stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse changeRole(
            Long memberId,
            Long projectId,
            Long projectMemberId,
            ProjectMemberRoleUpdateRequest request
    ) {
        Project project = findActiveProject(projectId);
        ProjectMember requester = findJoinedMember(memberId, projectId);
        validateOwner(requester);
        validateProjectInProgress(project);

        ProjectMember target = findJoinedProjectMember(projectMemberId, projectId);
        ProjectMemberRole newRole = request.getRole();

        if (target.getRole() == newRole) {
            return ProjectMemberResponse.from(target);
        }

        if (target.isOwner() && newRole == ProjectMemberRole.MEMBER) {
            validateNotLastOwner(projectId);
        }

        target.changeRole(newRole);
        return ProjectMemberResponse.from(target);
    }

    @Transactional
    public void leaveProject(Long memberId, Long projectId) {
        Project project = findActiveProject(projectId);
        ProjectMember projectMember = findJoinedMember(memberId, projectId);
        validateProjectInProgress(project);

        if (projectMember.isOwner()) {
            validateNotLastOwner(projectId);
        }

        projectMember.leave();
    }

    @Transactional
    public void removeMember(Long memberId, Long projectId, Long projectMemberId) {
        Project project = findActiveProject(projectId);
        ProjectMember requester = findJoinedMember(memberId, projectId);
        validateOwner(requester);
        validateProjectInProgress(project);

        ProjectMember target = findJoinedProjectMember(projectMemberId, projectId);
        if (target.isOwner()) {
            throw new CustomException(ErrorCode.PROJECT_OWNER_CANNOT_BE_REMOVED);
        }

        target.leave();
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

    private ProjectMember findJoinedProjectMember(Long projectMemberId, Long projectId) {
        return projectMemberRepository.findByIdAndProjectId(projectMemberId, projectId)
                .filter(ProjectMember::isJoined)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }

    private void validateOwner(ProjectMember projectMember) {
        if (!projectMember.isOwner()) {
            throw new CustomException(ErrorCode.PROJECT_OWNER_REQUIRED);
        }
    }

    private void validateProjectInProgress(Project project) {
        if (project.isCompleted()) {
            throw new CustomException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }
    }

    private void validateNotLastOwner(Long projectId) {
        long ownerCount = projectMemberRepository.countByProjectIdAndRoleAndStatus(
                projectId,
                ProjectMemberRole.OWNER,
                ProjectMemberStatus.JOINED
        );
        if (ownerCount <= 1) {
            throw new CustomException(ErrorCode.LAST_PROJECT_OWNER);
        }
    }
}
