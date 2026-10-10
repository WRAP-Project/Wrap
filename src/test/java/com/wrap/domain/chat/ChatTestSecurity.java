package com.wrap.domain.chat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.wrap.domain.member.entity.Member;
import com.wrap.global.security.MemberDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

final class ChatTestSecurity {

    private ChatTestSecurity() {
    }

    static RequestPostProcessor authenticatedAs(Long memberId) {
        Member member = Member.builder()
                .email("chat-test-" + memberId + "@example.com")
                .password("encoded-password")
                .nickname("chat-test-member")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return user(new MemberDetails(member));
    }
}
