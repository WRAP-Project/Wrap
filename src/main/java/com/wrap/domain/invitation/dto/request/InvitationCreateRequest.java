package com.wrap.domain.invitation.dto.request;

import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InvitationCreateRequest {

    @NotBlank(message = "초대 대상 이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotNull(message = "초대 역할은 필수입니다.")
    private ProjectMemberRole role;
}
