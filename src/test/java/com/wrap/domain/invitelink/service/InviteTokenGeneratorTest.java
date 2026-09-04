package com.wrap.domain.invitelink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InviteTokenGeneratorTest {

    private final InviteTokenGenerator tokenGenerator = new InviteTokenGenerator();

    @Test
    void URL에_안전한_랜덤_토큰과_SHA_256_해시를_생성한다() {
        GeneratedInviteToken generatedToken = tokenGenerator.generate();

        assertThat(generatedToken.rawToken()).matches("^[A-Za-z0-9_-]{43}$");
        assertThat(generatedToken.tokenHash()).matches("^[0-9a-f]{64}$");
        assertThat(generatedToken.tokenHash())
                .isEqualTo(tokenGenerator.hash(generatedToken.rawToken()));
    }

    @Test
    void 동일한_토큰은_동일한_해시로_변환한다() {
        assertThat(tokenGenerator.hash("test"))
                .isEqualTo("9f86d081884c7d659a2feaa0c55ad015"
                        + "a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }

    @Test
    void 빈_토큰은_해시로_변환할_수_없다() {
        assertThatThrownBy(() -> tokenGenerator.hash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("초대 링크 토큰은 필수입니다.");
    }
}
