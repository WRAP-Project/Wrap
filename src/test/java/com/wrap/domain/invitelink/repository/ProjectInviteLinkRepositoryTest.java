package com.wrap.domain.invitelink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class ProjectInviteLinkRepositoryTest {

    @Autowired
    private ProjectInviteLinkRepository inviteLinkRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 토큰_해시로_초대_링크를_조회한다() {
        Project project = projectRepository.save(project());
        Member creator = memberRepository.save(member());
        ProjectInviteLink savedInviteLink = inviteLinkRepository.saveAndFlush(
                ProjectInviteLink.create(project, creator, tokenHash('a'))
        );
        entityManager.clear();

        ProjectInviteLink inviteLink = inviteLinkRepository
                .findByTokenHash(savedInviteLink.getTokenHash())
                .orElseThrow();

        assertThat(inviteLink.getId()).isEqualTo(savedInviteLink.getId());
        assertThat(inviteLink.getProject().getId()).isEqualTo(project.getId());
        assertThat(inviteLink.getCreatedBy().getId()).isEqualTo(creator.getId());
    }

    @Test
    void 프로젝트의_활성_초대_링크를_조회한다() {
        Project project = projectRepository.save(project());
        Member creator = memberRepository.save(member());
        ProjectInviteLink savedInviteLink = inviteLinkRepository.saveAndFlush(
                ProjectInviteLink.create(project, creator, tokenHash('a'))
        );

        ProjectInviteLink inviteLink = inviteLinkRepository
                .findByProjectIdAndActiveTrue(project.getId())
                .orElseThrow();

        assertThat(inviteLink.getId()).isEqualTo(savedInviteLink.getId());
        assertThat(inviteLink.isActive()).isTrue();
    }

    @Test
    void 비활성화된_초대_링크는_활성_링크로_조회하지_않는다() {
        Project project = projectRepository.save(project());
        Member creator = memberRepository.save(member());
        ProjectInviteLink inviteLink = ProjectInviteLink.create(
                project,
                creator,
                tokenHash('a')
        );
        inviteLink.revoke(LocalDateTime.of(2026, 9, 1, 15, 0));
        inviteLinkRepository.saveAndFlush(inviteLink);

        assertThat(inviteLinkRepository.findByProjectIdAndActiveTrue(project.getId()))
                .isEmpty();
    }

    @Test
    void 프로젝트의_초대_링크_목록을_최신순으로_조회한다() {
        Project project = projectRepository.save(project());
        Member creator = memberRepository.save(member());
        ProjectInviteLink oldInviteLink = ProjectInviteLink.create(
                project,
                creator,
                tokenHash('a')
        );
        oldInviteLink.revoke(LocalDateTime.of(2026, 9, 1, 11, 0));
        ProjectInviteLink newInviteLink = ProjectInviteLink.create(
                project,
                creator,
                tokenHash('b')
        );
        inviteLinkRepository.saveAllAndFlush(List.of(oldInviteLink, newInviteLink));
        updateCreatedAt(oldInviteLink, LocalDateTime.of(2026, 9, 1, 10, 0));
        updateCreatedAt(newInviteLink, LocalDateTime.of(2026, 9, 1, 12, 0));
        entityManager.clear();

        List<ProjectInviteLink> inviteLinks = inviteLinkRepository
                .findAllByProjectIdOrderByCreatedAtDesc(project.getId());

        assertThat(inviteLinks)
                .extracting(ProjectInviteLink::getTokenHash)
                .containsExactly(tokenHash('b'), tokenHash('a'));
    }

    @Test
    void 프로젝트와_링크_ID로_비활성화_대상을_잠금_조회한다() {
        Project project = projectRepository.save(project());
        Member creator = memberRepository.save(member());
        ProjectInviteLink savedInviteLink = inviteLinkRepository.saveAndFlush(
                ProjectInviteLink.create(project, creator, tokenHash('a'))
        );
        entityManager.clear();

        ProjectInviteLink inviteLink = inviteLinkRepository.findByIdAndProjectIdForUpdate(
                savedInviteLink.getId(),
                project.getId()
        ).orElseThrow();

        assertThat(entityManager.getLockMode(inviteLink))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void 토큰_해시로_참여_대상을_잠금_조회한다() {
        Project project = projectRepository.save(project());
        Member creator = memberRepository.save(member());
        ProjectInviteLink savedInviteLink = inviteLinkRepository.saveAndFlush(
                ProjectInviteLink.create(project, creator, tokenHash('a'))
        );
        entityManager.clear();

        ProjectInviteLink inviteLink = inviteLinkRepository.findByTokenHashForUpdate(
                savedInviteLink.getTokenHash()
        ).orElseThrow();

        assertThat(inviteLink.getId()).isEqualTo(savedInviteLink.getId());
        assertThat(entityManager.getLockMode(inviteLink))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
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

    private Member member() {
        return Member.builder()
                .email("owner@example.com")
                .password("password1234")
                .nickname("owner")
                .build();
    }

    private String tokenHash(char value) {
        return String.valueOf(value).repeat(64);
    }

    private void updateCreatedAt(ProjectInviteLink inviteLink, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                        update project_invite_link
                        set created_at = :createdAt
                        where id = :id
                        """)
                .setParameter("createdAt", createdAt)
                .setParameter("id", inviteLink.getId())
                .executeUpdate();
    }
}
