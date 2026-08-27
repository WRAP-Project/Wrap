package com.wrap.domain.invitation.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import org.junit.jupiter.api.Test;

class InvitationTest {

    @Test
    void 초대를_생성하면_INVITED_상태가_된다() {
        Project project = project();
        Member inviter = member("owner@example.com", "owner");
        Member invitee = member("member@example.com", "member");

        Invitation invitation = Invitation.create(
                project,
                inviter,
                invitee,
                ProjectMemberRole.MEMBER
        );

        assertThat(invitation.getProject()).isSameAs(project);
        assertThat(invitation.getInviter()).isSameAs(inviter);
        assertThat(invitation.getInvitee()).isSameAs(invitee);
        assertThat(invitation.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.INVITED);
    }

    @Test
    void 프로젝트가_없으면_초대를_생성할_수_없다() {
        assertThatThrownBy(() -> Invitation.create(
                null,
                member("owner@example.com", "owner"),
                member("member@example.com", "member"),
                ProjectMemberRole.MEMBER
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 초대한_회원이_없으면_초대를_생성할_수_없다() {
        assertThatThrownBy(() -> Invitation.create(
                project(),
                null,
                member("member@example.com", "member"),
                ProjectMemberRole.MEMBER
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 초대_대상_회원이_없으면_초대를_생성할_수_없다() {
        assertThatThrownBy(() -> Invitation.create(
                project(),
                member("owner@example.com", "owner"),
                null,
                ProjectMemberRole.MEMBER
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 역할이_없으면_초대를_생성할_수_없다() {
        assertThatThrownBy(() -> Invitation.create(
                project(),
                member("owner@example.com", "owner"),
                member("member@example.com", "member"),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 초대를_수락하면_ACCEPTED_상태가_된다() {
        Invitation invitation = invitation();

        invitation.accept();

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    @Test
    void 이미_처리된_초대는_수락할_수_없다() {
        Invitation invitation = invitation();
        invitation.accept();

        assertThatThrownBy(invitation::accept)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("대기 중인 초대만 수락할 수 있습니다.");
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
    }

    private Invitation invitation() {
        return Invitation.create(
                project(),
                member("owner@example.com", "owner"),
                member("member@example.com", "member"),
                ProjectMemberRole.MEMBER
        );
    }

    private Project project() {
        return Project.create(
                "Wrap",
                null,
                null,
                null,
                null,
                null,
                Project.DEFAULT_COLOR
        );
    }

    private Member member(String email, String nickname) {
        return Member.builder()
                .email(email)
                .password("password1234")
                .nickname(nickname)
                .build();
    }
}
