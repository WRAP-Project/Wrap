package com.wrap.global.auth;

import com.wrap.global.exception.BusinessException;
import com.wrap.global.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionMemberResolver {

    public static final String MEMBER_ID = "memberId";

    public Long requireMemberId(HttpSession session) {
        Object memberId = session.getAttribute(MEMBER_ID);

        if (memberId instanceof Long id) {
            return id;
        }

        if (memberId instanceof Integer id) {
            return id.longValue();
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
