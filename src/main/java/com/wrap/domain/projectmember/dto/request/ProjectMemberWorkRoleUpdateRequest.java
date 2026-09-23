package com.wrap.domain.projectmember.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectMemberWorkRoleUpdateRequest {

    @NotNull(message = "변경할 프로젝트 업무 역할은 필수입니다.")
    @Size(max = 50, message = "프로젝트 업무 역할은 50자 이하여야 합니다.")
    private String workRole;
}
