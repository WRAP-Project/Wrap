package com.wrap.domain.projectmember.service;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.dto.response.ProjectMemberResponse;
import com.wrap.domain.projectmember.entity.ProjectMember;
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
}
