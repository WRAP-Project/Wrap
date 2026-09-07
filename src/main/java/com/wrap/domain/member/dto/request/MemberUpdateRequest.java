package com.wrap.domain.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberUpdateRequest {

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
    @Pattern(regexp = "^(?!\\s*$).+", message = "닉네임은 공백일 수 없습니다.")
    private String nickname;

    @Size(max = 50, message = "소속은 50자 이하여야 합니다.")
    private String team;

    @Size(max = 50, message = "역할은 50자 이하여야 합니다.")
    private String role;

    @Size(max = 200, message = "자기소개는 200자 이하여야 합니다.")
    private String bio;

    @Pattern(
            regexp = "^#[0-9a-fA-F]{6}$",
            message = "색상은 #RRGGBB 형식이어야 합니다."
    )
    private String accentColor;
}
