package com.wrap.domain.member.dto.response;

import com.wrap.domain.member.entity.Member;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {

    private Long id;
    private String email;
    private String nickname;
    private String team;
    private String role;
    private String bio;
    private String accentColor;
    private LocalDateTime createdAt;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .team(member.getTeam())
                .role(member.getRole())
                .bio(member.getBio())
                .accentColor(member.getAccentColor())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
