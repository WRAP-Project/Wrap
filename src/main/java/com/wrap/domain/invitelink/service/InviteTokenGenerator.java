package com.wrap.domain.invitelink.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class InviteTokenGenerator {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public GeneratedInviteToken generate() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        return new GeneratedInviteToken(rawToken, hash(rawToken));
    }

    public String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("초대 링크 토큰은 필수입니다.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
