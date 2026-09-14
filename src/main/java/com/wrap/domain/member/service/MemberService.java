package com.wrap.domain.member.service;

import com.wrap.domain.member.dto.request.LoginRequest;
import com.wrap.domain.member.dto.request.MemberUpdateRequest;
import com.wrap.domain.member.dto.request.SignupRequest;
import com.wrap.domain.member.dto.response.MemberResponse;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import com.wrap.global.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    @Transactional
    public MemberResponse signup(SignupRequest request, HttpServletRequest httpRequest) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        MemberResponse response = MemberResponse.from(memberRepository.save(member));

        // 회원가입은 인증을 부여하지 않는다.
        // 이전 사용자의 세션이 남아 있으면 새 회원이 그 사용자로 인식되므로 반드시 폐기한다.
        SecurityContextHolder.clearContext();
        invalidateCurrentSession(httpRequest);

        return response;
    }

    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        MemberDetails memberDetails = (MemberDetails) userDetailsService.loadUserByUsername(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), memberDetails.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberDetails, null, memberDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션 고정(Session Fixation) 방지.
        // formLogin을 끈 수동 로그인이라 Spring Security의 기본 세션 ID 재발급이 동작하지 않는다.
        // 기존 세션을 직접 폐기해야 이전 사용자의 세션이 재사용되지 않는다.
        invalidateCurrentSession(httpRequest);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
        );

        return MemberResponse.from(memberDetails.getMember());
    }

    @Transactional(readOnly = true)
    public MemberResponse getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        return MemberResponse.from(member);
    }

    @Transactional
    public MemberResponse updateMe(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        member.updateProfile(
                request.getNickname(),
                request.getTeam(),
                request.getRole(),
                request.getBio(),
                request.getAccentColor()
        );

        return MemberResponse.from(member);
    }

    public void logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        invalidateCurrentSession(httpRequest);
    }

    private void invalidateCurrentSession(HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
