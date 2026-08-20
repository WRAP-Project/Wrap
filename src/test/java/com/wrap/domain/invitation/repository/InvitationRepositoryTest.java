package com.wrap.domain.invitation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class InvitationRepositoryTest {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 프로젝트와_초대_대상으로_활성_초대_존재_여부를_조회한다() {
        Project project = projectRepository.save(project());
        Member inviter = memberRepository.save(member("owner@example.com", "owner"));
        Member invitee = memberRepository.save(member("member@example.com", "member"));
        invitationRepository.saveAndFlush(Invitation.create(
                project,
                inviter,
                invitee,
                ProjectMemberRole.MEMBER
        ));

        boolean exists = invitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                project.getId(),
                invitee.getId(),
                InvitationStatus.INVITED
        );

        assertThat(exists).isTrue();
    }

    @Test
    void 종료된_초대는_활성_초대로_조회하지_않는다() {
        Project project = projectRepository.save(project());
        Member inviter = memberRepository.save(member("owner@example.com", "owner"));
        Member invitee = memberRepository.save(member("member@example.com", "member"));
        Invitation invitation = Invitation.create(
                project,
                inviter,
                invitee,
                ProjectMemberRole.MEMBER
        );
        ReflectionTestUtils.setField(invitation, "status", InvitationStatus.ACCEPTED);
        invitationRepository.saveAndFlush(invitation);

        boolean exists = invitationRepository.existsByProjectIdAndInviteeIdAndStatus(
                project.getId(),
                invitee.getId(),
                InvitationStatus.INVITED
        );

        assertThat(exists).isFalse();
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
