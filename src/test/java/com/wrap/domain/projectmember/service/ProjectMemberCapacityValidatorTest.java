package com.wrap.domain.projectmember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.invitation.repository.InvitationRepository;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberCapacityValidatorTest {

    private static final Long PROJECT_ID = 10L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @InjectMocks
    private ProjectMemberCapacityValidator validator;

    @Test
    void joinedMembersAndValidInvitationsBelowLimitLeaveSeatAvailable() {
        given(projectMemberRepository.countByProjectIdAndStatus(
                PROJECT_ID,
                ProjectMemberStatus.JOINED
        )).willReturn(4L);
        given(invitationRepository.countByProjectIdAndStatusAndExpiresAtAfter(
                PROJECT_ID,
                InvitationStatus.INVITED,
                NOW
        )).willReturn(1L);

        assertThatCode(() -> validator.validateSeatAvailable(PROJECT_ID, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void joinedMembersAndValidInvitationsAtLimitRejectAdditionalSeat() {
        given(projectMemberRepository.countByProjectIdAndStatus(
                PROJECT_ID,
                ProjectMemberStatus.JOINED
        )).willReturn(4L);
        given(invitationRepository.countByProjectIdAndStatusAndExpiresAtAfter(
                PROJECT_ID,
                InvitationStatus.INVITED,
                NOW
        )).willReturn(2L);

        assertThatThrownBy(() -> validator.validateSeatAvailable(PROJECT_ID, NOW))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> {
                    CustomException customException = (CustomException) exception;
                    assertThat(customException.getErrorCode())
                            .isEqualTo(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
                });
    }
}
