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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

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
        assertThat(results).allSatisfy(membership -> assertThat(
                entityManager.getEntityManagerFactory()
                        .getPersistenceUnitUtil()
                        .isLoaded(membership.getProject())
        ).isTrue());
    }

    @Test
    void findsJoinedMemberProfilesForMultipleProjectsInStableOrder() {
        Project firstProject = project("First", ProjectStatus.IN_PROGRESS);
        Project secondProject = project("Second", ProjectStatus.IN_PROGRESS);
        LocalDateTime early = LocalDateTime.of(2026, 7, 1, 9, 0);
        LocalDateTime sameTime = LocalDateTime.of(2026, 7, 2, 9, 0);

        ProjectMember first = join(
                member("first@example.com", "첫째", null),
                firstProject,
                ProjectMemberRole.OWNER,
                early
        );
        ProjectMember second = join(
                member("second@example.com", "둘째", "https://example.com/second.png"),
                firstProject,
                ProjectMemberRole.MEMBER,
                sameTime
        );
        ProjectMember third = join(
                member("third@example.com", "셋째", null),
                firstProject,
                ProjectMemberRole.MEMBER,
                sameTime
        );
        ProjectMember left = join(
                member("left@example.com", "탈퇴", null),
                firstProject,
                ProjectMemberRole.MEMBER,
                early.minusDays(1)
        );
        left.leave();
        ProjectMember invited = join(
                member("invited@example.com", "초대", null),
                firstProject,
                ProjectMemberRole.MEMBER,
                early.minusDays(1)
        );
        ReflectionTestUtils.setField(invited, "status", ProjectMemberStatus.INVITED);
        ProjectMember otherProjectMember = join(
                member("other@example.com", "다른 프로젝트", null),
                secondProject,
                ProjectMemberRole.OWNER,
                early
        );

        entityManager.flush();
        entityManager.clear();

        List<ProjectMember> results =
                projectMemberRepository.findAllByProjectIdsAndStatusWithMember(
                        List.of(firstProject.getId(), secondProject.getId()),
                        ProjectMemberStatus.JOINED
                );

        assertThat(results).extracting(ProjectMember::getId)
                .containsExactly(
                        first.getId(),
                        second.getId(),
                        third.getId(),
                        otherProjectMember.getId()
                );
        assertThat(results).extracting(ProjectMember::getStatus)
                .containsOnly(ProjectMemberStatus.JOINED);
        assertThat(results).extracting(membership -> membership.getMember().getNickname())
                .containsExactly("첫째", "둘째", "셋째", "다른 프로젝트");
        assertThat(results.get(1).getMember().getProfileImage())
                .isEqualTo("https://example.com/second.png");
        assertThat(results.get(0).getMember().getProfileImage()).isNull();
        assertThat(results).allSatisfy(membership -> assertThat(
                entityManager.getEntityManagerFactory()
                        .getPersistenceUnitUtil()
                        .isLoaded(membership.getMember())
        ).isTrue());
    }

    @Test
    void savesAndLoadsWorkRole() {
        Member member = member("work-role@example.com");
        member.updateProfile(null, null, "  백엔드  ", null, null);
        Project project = project("Work role", ProjectStatus.IN_PROGRESS);

        ProjectMember saved = join(member, project, ProjectMemberRole.MEMBER);
        entityManager.flush();
        entityManager.clear();

        ProjectMember found = projectMemberRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getWorkRole()).isEqualTo("백엔드");
    }

    @Test
    void savesAndLoadsNullWorkRole() {
        Member member = member("no-work-role@example.com");
        Project project = project("No work role", ProjectStatus.IN_PROGRESS);

        ProjectMember saved = join(member, project, ProjectMemberRole.MEMBER);
        entityManager.flush();
        entityManager.clear();

        ProjectMember found = projectMemberRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getWorkRole()).isNull();
    }

    @Test
    void countsOnlyJoinedMembersForProjectCapacity() {
        Project project = project("Capacity", ProjectStatus.IN_PROGRESS);
        Project otherProject = project("Other", ProjectStatus.IN_PROGRESS);

        join(member("owner@example.com"), project, ProjectMemberRole.OWNER);
        join(member("joined@example.com"), project, ProjectMemberRole.MEMBER);

        ProjectMember left = join(
                member("left-capacity@example.com"),
                project,
                ProjectMemberRole.MEMBER
        );
        left.leave();

        ProjectMember invited = join(
                member("invited-capacity@example.com"),
                project,
                ProjectMemberRole.MEMBER
        );
        ReflectionTestUtils.setField(invited, "status", ProjectMemberStatus.INVITED);

        join(member("other-capacity@example.com"), otherProject, ProjectMemberRole.OWNER);
        entityManager.flush();
        entityManager.clear();

        long count = projectMemberRepository.countByProjectIdAndStatus(
                project.getId(),
                ProjectMemberStatus.JOINED
        );

        assertThat(count).isEqualTo(2);
    }

    private Member member(String email) {
        return member(email, "member", null);
    }

    private Member member(String email, String nickname, String profileImage) {
        Member member = Member.builder()
                .email(email)
                .password("encoded-password")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "profileImage", profileImage);
        return memberRepository.save(member);
    }

    private Project project(String name, ProjectStatus status) {
        Project project = Project.create(name, null, null, null, null, null, Project.DEFAULT_COLOR);
        if (status == ProjectStatus.COMPLETED) {
            project.complete(LocalDateTime.of(2026, 8, 31, 18, 0));
        }
        return projectRepository.save(project);
    }

    private ProjectMember join(Member member, Project project, ProjectMemberRole role) {
        return join(member, project, role, LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    private ProjectMember join(
            Member member,
            Project project,
            ProjectMemberRole role,
            LocalDateTime joinedAt
    ) {
        return projectMemberRepository.save(ProjectMember.join(
                member, project, role, joinedAt));
    }
}
