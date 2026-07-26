package com.wrap.domain.projectmember.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProjectMemberTest {

    private final Member member = mock(Member.class);
    private final Project project = mock(Project.class);
    private final LocalDateTime joinedAt = LocalDateTime.of(2026, 7, 26, 14, 0);

    @Test
    void 프로젝트_생성자는_OWNER이자_JOINED_멤버가_된다() {
        ProjectMember projectMember = ProjectMember.createOwner(member, project, joinedAt);

        assertEquals(ProjectMemberRole.OWNER, projectMember.getRole());
        assertEquals(ProjectMemberStatus.JOINED, projectMember.getStatus());
        assertEquals(joinedAt, projectMember.getJoinedAt());
        assertTrue(projectMember.isJoinedOwner());
    }

    @Test
    void 초대_수락_멤버는_지정된_역할로_참여한다() {
        ProjectMember projectMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                joinedAt
        );

        assertEquals(ProjectMemberRole.MEMBER, projectMember.getRole());
        assertTrue(projectMember.isJoined());
        assertFalse(projectMember.isOwner());
    }

    @Test
    void 참여_중인_멤버는_프로젝트를_나갈_수_있다() {
        ProjectMember projectMember = joinedMember();

        projectMember.leave();

        assertEquals(ProjectMemberStatus.LEFT, projectMember.getStatus());
        assertFalse(projectMember.isJoined());
    }

    @Test
    void 나간_멤버는_새_역할과_참여_시각으로_복구된다() {
        ProjectMember projectMember = joinedMember();
        projectMember.leave();
        LocalDateTime rejoinedAt = joinedAt.plusDays(1);

        projectMember.rejoin(ProjectMemberRole.OWNER, rejoinedAt);

        assertEquals(ProjectMemberStatus.JOINED, projectMember.getStatus());
        assertEquals(ProjectMemberRole.OWNER, projectMember.getRole());
        assertEquals(rejoinedAt, projectMember.getJoinedAt());
    }

    @Test
    void 참여_중인_멤버를_다시_참여시킬_수_없다() {
        ProjectMember projectMember = joinedMember();

        assertThrows(
                IllegalStateException.class,
                () -> projectMember.rejoin(ProjectMemberRole.OWNER, joinedAt.plusDays(1))
        );
    }

    @Test
    void 나간_멤버의_역할은_변경할_수_없다() {
        ProjectMember projectMember = joinedMember();
        projectMember.leave();

        assertThrows(
                IllegalStateException.class,
                () -> projectMember.changeRole(ProjectMemberRole.OWNER)
        );
    }

    @Test
    void 필수_연관관계가_없으면_프로젝트_멤버를_생성할_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectMember.join(
                        null,
                        project,
                        ProjectMemberRole.MEMBER,
                        joinedAt
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ProjectMember.join(
                        member,
                        null,
                        ProjectMemberRole.MEMBER,
                        joinedAt
                )
        );
    }

    private ProjectMember joinedMember() {
        return ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                joinedAt
        );
    }
}
