package com.wrap.domain.projectmember.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ProjectMemberRepositoryTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @NullSource
    @EnumSource(ProjectStatus.class)
    void findsOnlyMyJoinedNonDeletedProjectsWithOptionalStatus(ProjectStatus status) {
        Member me = member("me@example.com");
        Member other = member("other@example.com");
        Project inProgress = project("In progress", ProjectStatus.IN_PROGRESS);
        Project completed = project("Completed", ProjectStatus.COMPLETED);
        join(me, inProgress, ProjectMemberRole.OWNER);
        join(me, completed, ProjectMemberRole.MEMBER);

        for (ProjectStatus excludedStatus : ProjectStatus.values()) {
            Project deleted = project("Deleted " + excludedStatus, excludedStatus);
            deleted.softDelete(LocalDateTime.of(2026, 9, 1, 9, 0));
            join(me, deleted, ProjectMemberRole.OWNER);

            Project left = project("Left " + excludedStatus, excludedStatus);
            join(me, left, ProjectMemberRole.MEMBER).leave();

            Project others = project("Other member " + excludedStatus, excludedStatus);
            join(other, others, ProjectMemberRole.OWNER);
        }
        entityManager.flush();
        entityManager.clear();

        List<ProjectMember> results = status == null
                ? projectMemberRepository.findAllByMemberIdAndStatusAndProjectDeletedAtIsNull(
                        me.getId(), ProjectMemberStatus.JOINED)
                : projectMemberRepository.findAllByMemberIdAndStatusAndProjectStatusAndProjectDeletedAtIsNull(
                        me.getId(), ProjectMemberStatus.JOINED, status);

        Long[] expectedIds = status == null
                ? new Long[]{inProgress.getId(), completed.getId()}
                : new Long[]{status == ProjectStatus.IN_PROGRESS ? inProgress.getId() : completed.getId()};
        assertThat(results).extracting(membership -> membership.getProject().getId())
                .containsExactlyInAnyOrder(expectedIds);
    }

    private Member member(String email) {
        return memberRepository.save(Member.builder()
                .email(email).password("encoded-password").nickname("member").build());
    }

    private Project project(String name, ProjectStatus status) {
        Project project = Project.create(name, null, null, null, null, null, Project.DEFAULT_COLOR);
        if (status == ProjectStatus.COMPLETED) {
            project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        }
        return projectRepository.save(project);
    }

    private ProjectMember join(Member member, Project project, ProjectMemberRole role) {
        return projectMemberRepository.save(ProjectMember.join(
                member, project, role, LocalDateTime.of(2026, 7, 1, 9, 0)));
    }
}
