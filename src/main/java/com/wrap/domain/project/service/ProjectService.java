package com.wrap.domain.project.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
