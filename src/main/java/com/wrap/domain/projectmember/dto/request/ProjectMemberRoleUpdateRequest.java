package com.wrap.domain.projectmember.dto.request;

import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectMemberRoleUpdateRequest {

    @NotNull(message = "변경할 프로젝트 역할은 필수입니다.")
    private ProjectMemberRole role;
}
