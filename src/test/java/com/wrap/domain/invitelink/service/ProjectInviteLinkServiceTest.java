package com.wrap.domain.invitelink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkInfoResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteJoinResponse;
import com.wrap.domain.invitelink.dto.response.ProjectInviteLinkSummaryResponse;
import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import com.wrap.domain.invitelink.repository.ProjectInviteLinkRepository;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.projectmember.service.ProjectMemberValidator;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectInviteLinkServiceTest {

    private static final String RAW_TOKEN = "raw-token";
    private static final String TOKEN_HASH = "a".repeat(64);

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectInviteLinkRepository inviteLinkRepository;

    @Mock
    private ProjectMemberValidator projectMemberValidator;

    @Mock
    private InviteTokenGenerator tokenGenerator;

    private ProjectInviteLinkService inviteLinkService;

    @BeforeEach
    void setUp() {
        inviteLinkService = new ProjectInviteLinkService(
                projectRepository,
                memberRepository,
                projectMemberRepository,
                inviteLinkRepository,
                projectMemberValidator,
                tokenGenerator,
                "https://wrap-client.vercel.app/join/"
        );
    }

    @Test
    void 프로젝트_관리자가_초대_링크를_생성한다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByProjectIdAndActiveTrue(10L))
                .willReturn(Optional.empty());
        given(tokenGenerator.generate())
                .willReturn(new GeneratedInviteToken(RAW_TOKEN, TOKEN_HASH));
        given(inviteLinkRepository.existsByTokenHash(TOKEN_HASH)).willReturn(false);
        given(inviteLinkRepository.save(any(ProjectInviteLink.class)))
                .willAnswer(invocation -> savedInviteLink(invocation.getArgument(0)));

        ProjectInviteLinkResponse response = inviteLinkService.create(1L, 10L);

        assertThat(response.getInviteLinkId()).isEqualTo(100L);
        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getProjectName()).isEqualTo("Wrap");
        assertThat(response.getCreatedByMemberId()).isEqualTo(1L);
        assertThat(response.getInviteUrl())
                .isEqualTo("https://wrap-client.vercel.app/join/raw-token");
        assertThat(response.isActive()).isTrue();
        verify(projectMemberValidator).validateOwner(owner);
        verify(inviteLinkRepository).save(any(ProjectInviteLink.class));
    }

    @Test
    void 활성_초대_링크가_이미_있으면_새로_생성할_수_없다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        ProjectInviteLink activeInviteLink = ProjectInviteLink.create(
                project,
                ownerMember,
                TOKEN_HASH
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByProjectIdAndActiveTrue(10L))
                .willReturn(Optional.of(activeInviteLink));

        assertError(
                () -> inviteLinkService.create(1L, 10L),
                ErrorCode.INVITE_LINK_ALREADY_EXISTS
        );

        verify(tokenGenerator, never()).generate();
        verify(inviteLinkRepository, never()).save(any());
    }

    @Test
    void 완료된_프로젝트에는_초대_링크를_생성할_수_없다() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 9, 1, 11, 0));
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));

        assertError(
                () -> inviteLinkService.create(1L, 10L),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );

        verify(inviteLinkRepository, never()).save(any());
    }

    @Test
    void 프로젝트_관리자가_아니면_초대_링크를_생성할_수_없다() {
        Project project = project(10L);
        ProjectMember requester = ProjectMember.join(
                member(1L),
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(requester);
        willThrow(new CustomException(ErrorCode.PROJECT_OWNER_REQUIRED))
                .given(projectMemberValidator)
                .validateOwner(requester);

        assertError(
                () -> inviteLinkService.create(1L, 10L),
                ErrorCode.PROJECT_OWNER_REQUIRED
        );

        verify(projectRepository, never()).findByIdAndDeletedAtIsNullForUpdate(any());
        verify(inviteLinkRepository, never()).save(any());
    }

    @Test
    void 토큰_해시가_충돌하면_새로운_토큰을_다시_생성한다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        GeneratedInviteToken collidedToken = new GeneratedInviteToken(
                "collided-token",
                "a".repeat(64)
        );
        GeneratedInviteToken uniqueToken = new GeneratedInviteToken(
                "unique-token",
                "b".repeat(64)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByProjectIdAndActiveTrue(10L))
                .willReturn(Optional.empty());
        given(tokenGenerator.generate()).willReturn(collidedToken, uniqueToken);
        given(inviteLinkRepository.existsByTokenHash(collidedToken.tokenHash()))
                .willReturn(true);
        given(inviteLinkRepository.existsByTokenHash(uniqueToken.tokenHash()))
                .willReturn(false);
        given(inviteLinkRepository.save(any(ProjectInviteLink.class)))
                .willAnswer(invocation -> savedInviteLink(invocation.getArgument(0)));

        ProjectInviteLinkResponse response = inviteLinkService.create(1L, 10L);

        assertThat(response.getInviteUrl())
                .isEqualTo("https://wrap-client.vercel.app/join/unique-token");
        verify(tokenGenerator, times(2)).generate();
    }

    @Test
    void 고유한_토큰을_만들지_못하면_생성을_중단한다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        GeneratedInviteToken collidedToken = new GeneratedInviteToken(
                "collided-token",
                TOKEN_HASH
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByProjectIdAndActiveTrue(10L))
                .willReturn(Optional.empty());
        given(tokenGenerator.generate()).willReturn(collidedToken);
        given(inviteLinkRepository.existsByTokenHash(TOKEN_HASH)).willReturn(true);

        assertError(
                () -> inviteLinkService.create(1L, 10L),
                ErrorCode.INVITE_LINK_TOKEN_GENERATION_FAILED
        );

        verify(tokenGenerator, times(5)).generate();
        verify(inviteLinkRepository, never()).save(any());
    }

    @Test
    void 프로젝트_관리자가_초대_링크_목록을_조회한다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        ProjectInviteLink activeInviteLink = inviteLink(
                200L,
                project,
                ownerMember,
                "b".repeat(64),
                LocalDateTime.of(2026, 9, 1, 12, 0)
        );
        ProjectInviteLink revokedInviteLink = inviteLink(
                100L,
                project,
                ownerMember,
                "a".repeat(64),
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        revokedInviteLink.revoke(LocalDateTime.of(2026, 9, 1, 11, 0));
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(inviteLinkRepository.findAllByProjectIdOrderByCreatedAtDesc(10L))
                .willReturn(List.of(activeInviteLink, revokedInviteLink));

        List<ProjectInviteLinkSummaryResponse> responses =
                inviteLinkService.getInviteLinks(1L, 10L);

        assertThat(responses)
                .extracting(ProjectInviteLinkSummaryResponse::getInviteLinkId)
                .containsExactly(200L, 100L);
        assertThat(responses.get(0).isActive()).isTrue();
        assertThat(responses.get(1).isActive()).isFalse();
        assertThat(responses.get(1).getRevokedAt())
                .isEqualTo(LocalDateTime.of(2026, 9, 1, 11, 0));
        verify(projectMemberValidator).validateOwner(owner);
    }

    @Test
    void 초대_링크가_없으면_빈_목록을_반환한다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(inviteLinkRepository.findAllByProjectIdOrderByCreatedAtDesc(10L))
                .willReturn(List.of());

        List<ProjectInviteLinkSummaryResponse> responses =
                inviteLinkService.getInviteLinks(1L, 10L);

        assertThat(responses).isEmpty();
        verify(projectMemberValidator).validateOwner(owner);
    }

    @Test
    void 프로젝트_관리자가_초대_링크를_비활성화한다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                ownerMember,
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByIdAndProjectIdForUpdate(100L, 10L))
                .willReturn(Optional.of(inviteLink));

        inviteLinkService.revoke(1L, 10L, 100L);

        assertThat(inviteLink.isActive()).isFalse();
        assertThat(inviteLink.getRevokedAt()).isNotNull();
        verify(projectMemberValidator).validateOwner(owner);
    }

    @Test
    void 존재하지_않는_초대_링크는_비활성화할_수_없다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByIdAndProjectIdForUpdate(100L, 10L))
                .willReturn(Optional.empty());

        assertError(
                () -> inviteLinkService.revoke(1L, 10L, 100L),
                ErrorCode.INVITE_LINK_NOT_FOUND
        );
    }

    @Test
    void 이미_비활성화된_초대_링크는_다시_비활성화할_수_없다() {
        Project project = project(10L);
        Member ownerMember = member(1L);
        ProjectMember owner = ProjectMember.createOwner(
                ownerMember,
                project,
                LocalDateTime.of(2026, 9, 1, 10, 0)
        );
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                ownerMember,
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        inviteLink.revoke(LocalDateTime.of(2026, 9, 1, 12, 0));
        given(projectMemberValidator.findJoinedMember(1L, 10L)).willReturn(owner);
        given(projectRepository.findByIdAndDeletedAtIsNullForUpdate(10L))
                .willReturn(Optional.of(project));
        given(inviteLinkRepository.findByIdAndProjectIdForUpdate(100L, 10L))
                .willReturn(Optional.of(inviteLink));

        assertError(
                () -> inviteLinkService.revoke(1L, 10L, 100L),
                ErrorCode.INVITE_LINK_ALREADY_REVOKED
        );

        assertThat(inviteLink.getRevokedAt())
                .isEqualTo(LocalDateTime.of(2026, 9, 1, 12, 0));
    }

    @Test
    void 유효한_초대_링크의_프로젝트_정보를_조회한다() {
        Project project = project(10L);
        Member creator = member(1L);
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                creator,
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));

        ProjectInviteLinkInfoResponse response =
                inviteLinkService.getInviteLinkInfo(RAW_TOKEN);

        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getProjectName()).isEqualTo("Wrap");
        assertThat(response.getProjectColor()).isEqualTo(Project.DEFAULT_COLOR);
        assertThat(response.getInviterNickname()).isEqualTo("owner");
    }

    @Test
    void 존재하지_않는_토큰으로_프로젝트_정보를_조회할_수_없다() {
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.empty());

        assertError(
                () -> inviteLinkService.getInviteLinkInfo(RAW_TOKEN),
                ErrorCode.INVITE_LINK_NOT_FOUND
        );
    }

    @Test
    void 비활성화된_초대_링크의_프로젝트_정보를_조회할_수_없다() {
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project(10L),
                member(1L),
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        inviteLink.revoke(LocalDateTime.of(2026, 9, 1, 12, 0));
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));

        assertError(
                () -> inviteLinkService.getInviteLinkInfo(RAW_TOKEN),
                ErrorCode.INVITE_LINK_NOT_FOUND
        );
    }

    @Test
    void 완료된_프로젝트의_초대_링크_정보를_조회할_수_없다() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 9, 1, 12, 0));
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                member(1L),
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));

        assertError(
                () -> inviteLinkService.getInviteLinkInfo(RAW_TOKEN),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );
    }

    @Test
    void 삭제된_프로젝트의_초대_링크_정보를_조회할_수_없다() {
        Project project = project(10L);
        project.softDelete(LocalDateTime.of(2026, 9, 1, 12, 0));
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                member(1L),
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHash(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));

        assertError(
                () -> inviteLinkService.getInviteLinkInfo(RAW_TOKEN),
                ErrorCode.INVITE_LINK_NOT_FOUND
        );
    }

    @Test
    void 로그인한_회원이_초대_링크로_프로젝트에_참여한다() {
        Project project = project(10L);
        Member creator = member(1L);
        Member joiner = member(2L, "joiner@example.com", "joiner");
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                creator,
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));
        given(memberRepository.findByIdAndDeletedAtIsNull(2L))
                .willReturn(Optional.of(joiner));
        given(projectMemberRepository.findByMemberIdAndProjectId(2L, 10L))
                .willReturn(Optional.empty());
        given(projectMemberRepository.save(any(ProjectMember.class)))
                .willAnswer(invocation -> {
                    ProjectMember projectMember = invocation.getArgument(0);
                    ReflectionTestUtils.setField(projectMember, "id", 200L);
                    return projectMember;
                });

        ProjectInviteJoinResponse response = inviteLinkService.join(2L, RAW_TOKEN);

        assertThat(response.getProjectId()).isEqualTo(10L);
        assertThat(response.getProjectName()).isEqualTo("Wrap");
        assertThat(response.getProjectMemberId()).isEqualTo(200L);
        assertThat(response.getMemberId()).isEqualTo(2L);
        assertThat(response.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(response.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
        assertThat(response.getJoinedAt()).isNotNull();
    }

    @Test
    void 탈퇴한_회원은_초대_링크로_다시_참여한다() {
        Project project = project(10L);
        Member creator = member(1L);
        Member joiner = member(2L, "joiner@example.com", "joiner");
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                creator,
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        ProjectMember leftMember = ProjectMember.join(
                joiner,
                project,
                ProjectMemberRole.OWNER,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        ReflectionTestUtils.setField(leftMember, "id", 200L);
        leftMember.leave();
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));
        given(memberRepository.findByIdAndDeletedAtIsNull(2L))
                .willReturn(Optional.of(joiner));
        given(projectMemberRepository.findByMemberIdAndProjectId(2L, 10L))
                .willReturn(Optional.of(leftMember));

        ProjectInviteJoinResponse response = inviteLinkService.join(2L, RAW_TOKEN);

        assertThat(response.getProjectMemberId()).isEqualTo(200L);
        assertThat(response.getRole()).isEqualTo(ProjectMemberRole.MEMBER);
        assertThat(response.getStatus()).isEqualTo(ProjectMemberStatus.JOINED);
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void 이미_참여_중인_회원은_초대_링크로_중복_참여할_수_없다() {
        Project project = project(10L);
        Member creator = member(1L);
        Member joiner = member(2L, "joiner@example.com", "joiner");
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                creator,
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        ProjectMember joinedMember = ProjectMember.join(
                joiner,
                project,
                ProjectMemberRole.MEMBER,
                LocalDateTime.of(2026, 8, 1, 10, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));
        given(memberRepository.findByIdAndDeletedAtIsNull(2L))
                .willReturn(Optional.of(joiner));
        given(projectMemberRepository.findByMemberIdAndProjectId(2L, 10L))
                .willReturn(Optional.of(joinedMember));

        assertError(
                () -> inviteLinkService.join(2L, RAW_TOKEN),
                ErrorCode.PROJECT_MEMBER_ALREADY_EXISTS
        );

        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void 비활성화된_초대_링크로_프로젝트에_참여할_수_없다() {
        Project project = project(10L);
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                member(1L),
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        inviteLink.revoke(LocalDateTime.of(2026, 9, 1, 12, 0));
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));

        assertError(
                () -> inviteLinkService.join(2L, RAW_TOKEN),
                ErrorCode.INVITE_LINK_NOT_FOUND
        );

        verify(memberRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void 삭제된_회원은_초대_링크로_프로젝트에_참여할_수_없다() {
        Project project = project(10L);
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                member(1L),
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));
        given(memberRepository.findByIdAndDeletedAtIsNull(2L))
                .willReturn(Optional.empty());

        assertError(
                () -> inviteLinkService.join(2L, RAW_TOKEN),
                ErrorCode.MEMBER_NOT_FOUND
        );

        verify(projectMemberRepository, never())
                .findByMemberIdAndProjectId(any(), any());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void 완료된_프로젝트에는_초대_링크로_참여할_수_없다() {
        Project project = project(10L);
        project.complete(LocalDateTime.of(2026, 9, 1, 12, 0));
        ProjectInviteLink inviteLink = inviteLink(
                100L,
                project,
                member(1L),
                TOKEN_HASH,
                LocalDateTime.of(2026, 9, 1, 11, 0)
        );
        given(tokenGenerator.hash(RAW_TOKEN)).willReturn(TOKEN_HASH);
        given(inviteLinkRepository.findByTokenHashForUpdate(TOKEN_HASH))
                .willReturn(Optional.of(inviteLink));

        assertError(
                () -> inviteLinkService.join(2L, RAW_TOKEN),
                ErrorCode.PROJECT_ALREADY_COMPLETED
        );

        verify(memberRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(projectMemberRepository, never()).save(any());
    }

    private ProjectInviteLink savedInviteLink(ProjectInviteLink inviteLink) {
        ReflectionTestUtils.setField(inviteLink, "id", 100L);
        ReflectionTestUtils.setField(
                inviteLink,
                "createdAt",
                LocalDateTime.of(2026, 9, 1, 12, 0)
        );
        return inviteLink;
    }

    private ProjectInviteLink inviteLink(
            Long id,
            Project project,
            Member creator,
            String tokenHash,
            LocalDateTime createdAt
    ) {
        ProjectInviteLink inviteLink = ProjectInviteLink.create(project, creator, tokenHash);
        ReflectionTestUtils.setField(inviteLink, "id", id);
        ReflectionTestUtils.setField(inviteLink, "createdAt", createdAt);
        return inviteLink;
    }

    private Project project(Long id) {
        Project project = Project.create(
                "Wrap",
                null,
                null,
                null,
                null,
                null,
                Project.DEFAULT_COLOR
        );
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }

    private Member member(Long id) {
        return member(id, "owner@example.com", "owner");
    }

    private Member member(Long id, String email, String nickname) {
        Member member = Member.builder()
                .email(email)
                .password("password1234")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(errorCode));
    }
}
