package com.wrap.domain.projectmember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import com.wrap.domain.invitation.dto.request.InvitationCreateRequest;
import com.wrap.domain.invitation.dto.response.InvitationResponse;
import com.wrap.domain.invitation.enums.InvitationStatus;
import com.wrap.domain.invitation.repository.InvitationRepository;
import com.wrap.domain.invitation.service.InvitationService;
import com.wrap.domain.invitelink.entity.ProjectInviteLink;
import com.wrap.domain.invitelink.repository.ProjectInviteLinkRepository;
import com.wrap.domain.invitelink.service.InviteTokenGenerator;
import com.wrap.domain.invitelink.service.ProjectInviteLinkService;
import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.config.TimeConfig;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(
        showSql = false,
        properties = "app.invite-link.base-url=https://wrap-client.vercel.app/join"
)
@Import({
        InvitationService.class,
        ProjectInviteLinkService.class,
        ProjectMemberCapacityValidator.class,
        ProjectMemberValidator.class,
        InviteTokenGenerator.class,
        TimeConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProjectMemberCapacityConcurrencyTest {

    private static final String RAW_TOKEN = "capacity-concurrency-token";

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private ProjectInviteLinkService inviteLinkService;

    @MockitoSpyBean
    private ProjectRepository projects;

    @Autowired
    private MemberRepository members;

    @Autowired
    private ProjectMemberRepository projectMembers;

    @Autowired
    private InvitationRepository invitations;

    @Autowired
    private ProjectInviteLinkRepository inviteLinks;

    @Autowired
    private InviteTokenGenerator tokenGenerator;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    @AfterEach
    void cleanUp() {
        transaction().executeWithoutResult(status -> {
            invitations.deleteAllInBatch();
            inviteLinks.deleteAllInBatch();
            projectMembers.deleteAllInBatch();
            projects.deleteAllInBatch();
            members.deleteAllInBatch();
        });
    }

    @Test
    void concurrentEmailInvitationsReserveOnlyLastAvailableSeat() throws Exception {
        Fixture fixture = fixture();

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> createInvitation(fixture, fixture.firstCandidateEmail()),
                () -> createInvitation(fixture, fixture.secondCandidateEmail())
        );

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
        assertCapacity(fixture.projectId(), 5, 1);
    }

    @Test
    void concurrentInviteLinkJoinsAddOnlyOneMember() throws Exception {
        Fixture fixture = fixture();

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> inviteLinkService.join(fixture.firstCandidateId(), RAW_TOKEN),
                () -> inviteLinkService.join(fixture.secondCandidateId(), RAW_TOKEN)
        );

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
        assertCapacity(fixture.projectId(), 6, 0);
    }

    @Test
    void emailInvitationAndInviteLinkJoinShareSameLastSeat() throws Exception {
        Fixture fixture = fixture();

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> createInvitation(fixture, fixture.firstCandidateEmail()),
                () -> inviteLinkService.join(fixture.secondCandidateId(), RAW_TOKEN)
        );

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
        assertCapacity(fixture.projectId(), 5, 1);
    }

    @Test
    void inviteLinkJoinAndEmailInvitationShareSameLastSeat() throws Exception {
        Fixture fixture = fixture();

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> inviteLinkService.join(fixture.firstCandidateId(), RAW_TOKEN),
                () -> createInvitation(fixture, fixture.secondCandidateEmail())
        );

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
        assertCapacity(fixture.projectId(), 6, 0);
    }

    @Test
    void acceptingReservedInvitationDoesNotOpenExtraSeatForConcurrentLinkJoin()
            throws Exception {
        Fixture fixture = fixture();
        InvitationResponse invitation = createInvitation(
                fixture,
                fixture.firstCandidateEmail()
        );

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> invitationService.accept(
                        fixture.firstCandidateId(),
                        invitation.getInvitationId()
                ),
                () -> inviteLinkService.join(fixture.secondCandidateId(), RAW_TOKEN)
        );

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_MEMBER_LIMIT_EXCEEDED);
        assertCapacity(fixture.projectId(), 6, 0);
        transaction().executeWithoutResult(status -> assertThat(
                invitations.findById(invitation.getInvitationId()).orElseThrow().getStatus()
        ).isEqualTo(InvitationStatus.ACCEPTED));
    }

    private ErrorCode runOverlapping(
            Long projectId,
            Runnable firstAction,
            Runnable secondAction
    ) throws Exception {
        CountDownLatch firstChanged = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch secondLockAttempt = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ErrorCode> first = executor.submit(() -> executeRequest(() -> {
                firstAction.run();
                firstChanged.countDown();
                await(allowFirstCommit);
            }));
            await(firstChanged);

            var repositoryAnswer = mockingDetails(projects)
                    .getMockCreationSettings()
                    .getDefaultAnswer();
            doAnswer(invocation -> {
                secondLockAttempt.countDown();
                return repositoryAnswer.answer(invocation);
            }).when(projects).findByIdAndDeletedAtIsNullForUpdate(projectId);

            Future<ErrorCode> second = executor.submit(() -> executeRequest(secondAction));
            await(secondLockAttempt);

            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            allowFirstCommit.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isNull();
            return second.get(10, TimeUnit.SECONDS);
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private ErrorCode executeRequest(Runnable action) {
        try {
            transaction().executeWithoutResult(status -> action.run());
            return null;
        } catch (CustomException exception) {
            return exception.getErrorCode();
        }
    }

    private Fixture fixture() {
        return transaction().execute(status -> {
            Project project = projects.save(Project.create(
                    "Capacity",
                    null,
                    null,
                    null,
                    null,
                    null,
                    Project.DEFAULT_COLOR
            ));
            Member owner = saveMember("owner");
            projectMembers.save(ProjectMember.createOwner(owner, project, now()));
            for (int index = 0; index < 4; index++) {
                Member joinedMember = saveMember("joined-" + index);
                projectMembers.save(ProjectMember.join(
                        joinedMember,
                        project,
                        ProjectMemberRole.MEMBER,
                        now()
                ));
            }

            Member firstCandidate = saveMember("candidate-1");
            Member secondCandidate = saveMember("candidate-2");
            inviteLinks.save(ProjectInviteLink.create(
                    project,
                    owner,
                    tokenGenerator.hash(RAW_TOKEN),
                    now()
            ));

            return new Fixture(
                    project.getId(),
                    owner.getId(),
                    firstCandidate.getId(),
                    firstCandidate.getEmail(),
                    secondCandidate.getId(),
                    secondCandidate.getEmail()
            );
        });
    }

    private InvitationResponse createInvitation(Fixture fixture, String email) {
        InvitationCreateRequest request = new InvitationCreateRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "role", ProjectMemberRole.MEMBER);
        return invitationService.create(fixture.ownerId(), fixture.projectId(), request);
    }

    private Member saveMember(String nickname) {
        return members.save(Member.builder()
                .email(UUID.randomUUID() + "@example.com")
                .password("encoded-password")
                .nickname(nickname)
                .build());
    }

    private void assertCapacity(
            Long projectId,
            long expectedJoinedCount,
            long expectedInvitationCount
    ) {
        transaction().executeWithoutResult(status -> {
            long joinedCount = projectMembers.countByProjectIdAndStatus(
                    projectId,
                    ProjectMemberStatus.JOINED
            );
            long invitationCount = invitations.countByProjectIdAndStatusAndExpiresAtAfter(
                    projectId,
                    InvitationStatus.INVITED,
                    now()
            );
            assertThat(joinedCount).isEqualTo(expectedJoinedCount);
            assertThat(invitationCount).isEqualTo(expectedInvitationCount);
            assertThat(joinedCount + invitationCount)
                    .isEqualTo(ProjectMemberCapacityValidator.MAX_CAPACITY);
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent request did not reach the expected point");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while coordinating concurrent requests", exception);
        }
    }

    private record Fixture(
            Long projectId,
            Long ownerId,
            Long firstCandidateId,
            String firstCandidateEmail,
            Long secondCandidateId,
            String secondCandidateEmail
    ) {
    }
}
