package com.wrap.domain.member.service;

import com.wrap.domain.member.dto.request.LoginRequest;
import com.wrap.domain.member.dto.request.SignupRequest;
import com.wrap.domain.member.dto.response.MemberResponse;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import com.wrap.global.security.MemberDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private MemberService memberService;

    @AfterEach
    void clearSecurityContext() {
        // SecurityContextHolder는 ThreadLocal이라 정리하지 않으면 다른 테스트로 인증 상태가 샌다.
        SecurityContextHolder.clearContext();
    }

    private SignupRequest signupRequest(String email, String password, String nickname) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private Member member(Long id, String email, String encodedPassword, String nickname) {
        Member member = Member.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        SignupRequest request = signupRequest("test@test.com", "password123", "테스터");
        Member saved = member(1L, "test@test.com", "encoded", "테스터");

        given(memberRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willReturn(saved);

        MemberResponse response = memberService.signup(request, new MockHttpServletRequest());

        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getNickname()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_duplicateEmail() {
        SignupRequest request = signupRequest("test@test.com", "password123", "테스터");
        given(memberRepository.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request, new MockHttpServletRequest()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        LoginRequest request = loginRequest("test@test.com", "password123");
        MemberDetails memberDetails = new MemberDetails(member(1L, "test@test.com", "encoded", "테스터"));
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        given(userDetailsService.loadUserByUsername("test@test.com")).willReturn(memberDetails);
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
        given(httpRequest.getSession(false)).willReturn(null);
        given(httpRequest.getSession(true)).willReturn(session);

        MemberResponse response = memberService.login(request, httpRequest);

        assertThat(response.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword() {
        LoginRequest request = loginRequest("test@test.com", "wrongPassword");
        MemberDetails memberDetails = new MemberDetails(member(1L, "test@test.com", "encoded", "테스터"));
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        given(userDetailsService.loadUserByUsername("test@test.com")).willReturn(memberDetails);
        given(passwordEncoder.matches("wrongPassword", "encoded")).willReturn(false);

        assertThatThrownBy(() -> memberService.login(request, httpRequest))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.LOGIN_FAILED));
    }

    @Test
    @DisplayName("로그아웃 성공 - 세션 무효화")
    void logout_withSession() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        given(httpRequest.getSession(false)).willReturn(session);

        memberService.logout(httpRequest);

        verify(session).invalidate();
    }

    @Test
    @DisplayName("로그아웃 - 세션 없을 때 예외 없이 처리")
    void logout_withoutSession() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        given(httpRequest.getSession(false)).willReturn(null);

        assertThatNoException().isThrownBy(() -> memberService.logout(httpRequest));
    }

    @Test
    @DisplayName("로그인 - 기존 세션을 폐기하고 새 세션을 발급한다")
    void login_invalidatesPreviousSession() {
        LoginRequest request = loginRequest("b@test.com", "password123");
        MemberDetails memberDetails = new MemberDetails(member(2L, "b@test.com", "encoded", "b"));
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession previousSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);

        given(userDetailsService.loadUserByUsername("b@test.com")).willReturn(memberDetails);
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
        given(httpRequest.getSession(false)).willReturn(previousSession);
        given(httpRequest.getSession(true)).willReturn(newSession);

        memberService.login(request, httpRequest);

        verify(previousSession).invalidate();
        verify(newSession).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
    }

    @Test
    @DisplayName("회원가입 - 이전 사용자의 세션을 폐기한다")
    void signup_invalidatesPreviousSession() {
        SignupRequest request = signupRequest("b@test.com", "password123", "b");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpSession previousSession = mock(HttpSession.class);

        given(memberRepository.existsByEmail("b@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(memberRepository.save(any(Member.class)))
                .willReturn(member(2L, "b@test.com", "encoded", "b"));
        given(httpRequest.getSession(false)).willReturn(previousSession);

        memberService.signup(request, httpRequest);

        verify(previousSession).invalidate();
    }

    @Test
    @DisplayName("회원가입 실패 시 기존 세션을 건드리지 않는다")
    void signup_duplicateEmail_keepsSession() {
        SignupRequest request = signupRequest("a@test.com", "password123", "a");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        given(memberRepository.existsByEmail("a@test.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request, httpRequest))
                .isInstanceOf(CustomException.class);

        verify(httpRequest, never()).getSession(anyBoolean());
    }
}
