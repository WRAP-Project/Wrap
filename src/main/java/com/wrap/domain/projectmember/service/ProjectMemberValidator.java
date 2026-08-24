package com.wrap.domain.projectmember.service;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMemberValidator {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public Project findActiveProject(Long projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
    }

    public ProjectMember findJoinedMember(Long memberId, Long projectId) {
        findActiveProject(projectId);
        return projectMemberRepository.findByMemberIdAndProjectIdAndStatus(
                        memberId,
                        projectId,
                        ProjectMemberStatus.JOINED
                )
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_ACCESS_DENIED));
    }

    public ProjectMember findJoinedProjectMember(Long projectId, Long projectMemberId) {
        findActiveProject(projectId);
        return projectMemberRepository.findByIdAndProjectId(projectMemberId, projectId)
                .filter(ProjectMember::isJoined)
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
    }

    public void validateOwner(ProjectMember projectMember) {
        if (!projectMember.isOwner()) {
            throw new CustomException(ErrorCode.PROJECT_OWNER_REQUIRED);
        }
    }
}
