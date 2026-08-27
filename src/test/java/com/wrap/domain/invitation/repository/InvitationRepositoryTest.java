package com.wrap.domain.invitation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
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

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void 초대_대상의_받은_초대_목록을_최신순으로_조회한다() {
        Project oldProject = projectRepository.save(project("Old Project"));
        Project newProject = projectRepository.save(project("New Project"));
        Member inviter = memberRepository.save(member("owner@example.com", "owner"));
        Member invitee = memberRepository.save(member("invitee@example.com", "invitee"));
        Member otherInvitee = memberRepository.save(member("other@example.com", "other"));

        Invitation oldInvitation = invitation(
                oldProject,
                inviter,
                invitee
        );
        Invitation newInvitation = invitation(
                newProject,
                inviter,
                invitee
        );
        Invitation otherInvitation = invitation(
                newProject,
                inviter,
                otherInvitee
        );
        invitationRepository.saveAllAndFlush(List.of(
                oldInvitation,
                newInvitation,
                otherInvitation
        ));
        updateCreatedAt(oldInvitation, LocalDateTime.of(2026, 8, 17, 10, 0));
        updateCreatedAt(newInvitation, LocalDateTime.of(2026, 8, 18, 10, 0));
        updateCreatedAt(otherInvitation, LocalDateTime.of(2026, 8, 19, 10, 0));
        entityManager.clear();

        List<Invitation> invitations =
                invitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(invitee.getId());

        assertThat(invitations)
                .extracting(Invitation::getProject)
                .extracting(Project::getName)
                .containsExactly("New Project", "Old Project");
    }

    @Test
    void 받은_초대가_없으면_빈_목록을_반환한다() {
        List<Invitation> invitations =
                invitationRepository.findAllByInviteeIdOrderByCreatedAtDesc(999L);

        assertThat(invitations).isEmpty();
    }

    @Test
    void 프로젝트의_보낸_초대_목록을_최신순으로_조회한다() {
        Project project = projectRepository.save(project("Wrap"));
        Project otherProject = projectRepository.save(project("Other Project"));
        Member inviter = memberRepository.save(member("owner@example.com", "owner"));
        Member oldInvitee = memberRepository.save(member("old@example.com", "old"));
        Member newInvitee = memberRepository.save(member("new@example.com", "new"));
        Member otherInvitee = memberRepository.save(member("other@example.com", "other"));

        Invitation oldInvitation = invitation(project, inviter, oldInvitee);
        Invitation newInvitation = invitation(project, inviter, newInvitee);
        Invitation otherInvitation = invitation(otherProject, inviter, otherInvitee);
        invitationRepository.saveAllAndFlush(List.of(
                oldInvitation,
                newInvitation,
                otherInvitation
        ));
        updateCreatedAt(oldInvitation, LocalDateTime.of(2026, 8, 17, 10, 0));
        updateCreatedAt(newInvitation, LocalDateTime.of(2026, 8, 18, 10, 0));
        updateCreatedAt(otherInvitation, LocalDateTime.of(2026, 8, 19, 10, 0));
        entityManager.clear();

        List<Invitation> invitations =
                invitationRepository.findAllByProjectIdOrderByCreatedAtDesc(project.getId());

        assertThat(invitations)
                .extracting(Invitation::getInvitee)
                .extracting(Member::getEmail)
                .containsExactly("new@example.com", "old@example.com");
    }

    @Test
    void 프로젝트에서_보낸_초대가_없으면_빈_목록을_반환한다() {
        List<Invitation> invitations =
                invitationRepository.findAllByProjectIdOrderByCreatedAtDesc(999L);

        assertThat(invitations).isEmpty();
    }

    @Test
    void 초대_ID로_수락_대상을_잠금_조회한다() {
        Project project = projectRepository.save(project());
        Member inviter = memberRepository.save(member("owner@example.com", "owner"));
        Member invitee = memberRepository.save(member("member@example.com", "member"));
        Invitation savedInvitation = invitationRepository.saveAndFlush(Invitation.create(
                project,
                inviter,
                invitee,
                ProjectMemberRole.MEMBER
        ));
        entityManager.clear();

        Invitation invitation = invitationRepository.findByIdForUpdate(savedInvitation.getId())
                .orElseThrow();

        assertThat(invitation.getProject().getId()).isEqualTo(project.getId());
        assertThat(invitation.getInvitee().getId()).isEqualTo(invitee.getId());
        assertThat(entityManager.getLockMode(invitation))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private Project project() {
        return project("Wrap");
    }

    private Project project(String name) {
        return Project.create(
                name,
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

    private Invitation invitation(
            Project project,
            Member inviter,
            Member invitee
    ) {
        return Invitation.create(
                project,
                inviter,
                invitee,
                ProjectMemberRole.MEMBER
        );
    }

    private void updateCreatedAt(Invitation invitation, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                        update invitation
                        set created_at = :createdAt
                        where id = :id
                        """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", invitation.getId())
                .executeUpdate();
    }
}
