package com.wrap.domain.invitelink.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProjectInviteLinkTest {

    private static final String TOKEN_HASH = "a".repeat(64);

    @Test
    void 초대_링크를_생성하면_활성_상태가_된다() {
        Project project = project();
        Member creator = member();

        ProjectInviteLink inviteLink = ProjectInviteLink.create(
                project,
                creator,
                TOKEN_HASH
        );

        assertThat(inviteLink.getProject()).isSameAs(project);
        assertThat(inviteLink.getCreatedBy()).isSameAs(creator);
        assertThat(inviteLink.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(inviteLink.isActive()).isTrue();
        assertThat(inviteLink.getRevokedAt()).isNull();
    }

    @Test
    void 초대_링크를_비활성화할_수_있다() {
        ProjectInviteLink inviteLink = inviteLink();
        LocalDateTime revokedAt = LocalDateTime.of(2026, 9, 1, 15, 0);

        inviteLink.revoke(revokedAt);

        assertThat(inviteLink.isActive()).isFalse();
        assertThat(inviteLink.getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void 이미_비활성화된_초대_링크는_다시_비활성화할_수_없다() {
        ProjectInviteLink inviteLink = inviteLink();
        inviteLink.revoke(LocalDateTime.of(2026, 9, 1, 15, 0));

        assertThatThrownBy(() -> inviteLink.revoke(
                LocalDateTime.of(2026, 9, 1, 16, 0)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 비활성화된 초대 링크입니다.");
    }

    @Test
    void 필수_값이_없으면_초대_링크를_생성할_수_없다() {
        assertThatThrownBy(() -> ProjectInviteLink.create(null, member(), TOKEN_HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProjectInviteLink.create(project(), null, TOKEN_HASH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProjectInviteLink.create(project(), member(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void SHA_256_형식이_아닌_토큰_해시는_저장할_수_없다() {
        assertThatThrownBy(() -> ProjectInviteLink.create(
                project(),
                member(),
                "invalid-token-hash"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("초대 링크 토큰 해시는 SHA-256 형식이어야 합니다.");
    }

    private ProjectInviteLink inviteLink() {
        return ProjectInviteLink.create(project(), member(), TOKEN_HASH);
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
}
