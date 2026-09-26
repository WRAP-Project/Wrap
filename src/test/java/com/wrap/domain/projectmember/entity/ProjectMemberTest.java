package com.wrap.domain.projectmember.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(member.getRole()).thenReturn("  디자이너  ");

        ProjectMember projectMember = ProjectMember.createOwner(member, project, joinedAt);

        assertEquals(ProjectMemberRole.OWNER, projectMember.getRole());
        assertEquals("디자이너", projectMember.getWorkRole());
        assertEquals(ProjectMemberStatus.JOINED, projectMember.getStatus());
        assertEquals(joinedAt, projectMember.getJoinedAt());
        assertTrue(projectMember.isJoinedOwner());
    }

    @Test
    void 초대_수락_멤버는_지정된_역할로_참여한다() {
        when(member.getRole()).thenReturn("프론트엔드");

        ProjectMember projectMember = ProjectMember.join(
                member,
                project,
                ProjectMemberRole.MEMBER,
                joinedAt
        );

        assertEquals(ProjectMemberRole.MEMBER, projectMember.getRole());
        assertEquals("프론트엔드", projectMember.getWorkRole());
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
        when(member.getRole()).thenReturn("디자인");
        ProjectMember projectMember = joinedMember();
        projectMember.leave();
        LocalDateTime rejoinedAt = joinedAt.plusDays(1);

        when(member.getRole()).thenReturn("개발");

        projectMember.rejoin(ProjectMemberRole.OWNER, rejoinedAt);

        assertEquals(ProjectMemberStatus.JOINED, projectMember.getStatus());
        assertEquals(ProjectMemberRole.OWNER, projectMember.getRole());
        assertEquals("디자인", projectMember.getWorkRole());
        assertEquals(rejoinedAt, projectMember.getJoinedAt());
    }

    @Test
    void 기본_업무_역할이_없거나_공백이면_역할_미지정으로_참여한다() {
        ProjectMember withoutRole = ProjectMember.createOwner(member, project, joinedAt);
        assertNull(withoutRole.getWorkRole());

        when(member.getRole()).thenReturn("   ");
        ProjectMember withBlankRole = ProjectMember.createOwner(member, project, joinedAt);
        assertNull(withBlankRole.getWorkRole());
    }

    @Test
    void 참여_중인_멤버는_업무_역할을_변경하거나_삭제할_수_있다() {
        ProjectMember projectMember = joinedMember();

        projectMember.changeWorkRole("  백엔드 개발  ");
        assertEquals("백엔드 개발", projectMember.getWorkRole());

        projectMember.changeWorkRole("   ");
        assertNull(projectMember.getWorkRole());
    }

    @Test
    void 업무_역할은_null이거나_50자를_초과할_수_없다() {
        ProjectMember projectMember = joinedMember();

        assertThrows(
                IllegalArgumentException.class,
                () -> projectMember.changeWorkRole(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> projectMember.changeWorkRole("가".repeat(51))
        );
    }

    @Test
    void 나간_멤버의_업무_역할은_변경할_수_없다() {
        ProjectMember projectMember = joinedMember();
        projectMember.leave();

        assertThrows(
                IllegalStateException.class,
                () -> projectMember.changeWorkRole("기획")
        );
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
