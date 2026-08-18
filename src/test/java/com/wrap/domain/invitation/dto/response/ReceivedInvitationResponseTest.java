package com.wrap.domain.invitation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.wrap.domain.invitation.entity.Invitation;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReceivedInvitationResponseTest {

    @Test
    void 초대_엔티티를_받은_초대_응답으로_변환한다() {
        Project project = Project.create(
                "Wrap",
                null,
                null,
                null,
                null,
                null,
                Project.DEFAULT_COLOR
        );
        ReflectionTestUtils.setField(project, "id", 10L);
        Member inviter = Member.builder()
                .email("owner@example.com")
                .password("password1234")
                .nickname("owner")
                .build();
        Invitation invitation = Invitation.create(
                project,
                inviter,
                member(),
                ProjectMemberRole.MEMBER
        );
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 18, 10, 0);
        ReflectionTestUtils.setField(invitation, "id", 100L);
        ReflectionTestUtils.setField(invitation, "createdAt", createdAt);

        ReceivedInvitationResponse response = ReceivedInvitationResponse.from(invitation);

        assertThat(response.getInvitationId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getProjectName()).isEqualTo("Wrap");
        assertThat(response.getInviterNickname()).isEqualTo("owner");
        assertThat(response.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(response.getStatus()).isEqualTo(InvitationStatus.INVITED);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    private Member member() {
        return Member.builder()
                .email("invitee@example.com")
                .password("password1234")
                .nickname("invitee")
                .build();
    }
}
