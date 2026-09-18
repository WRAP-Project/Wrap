package com.wrap.domain.projectmember.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.dto.request.ProjectMemberRoleUpdateRequest;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.enums.ProjectMemberRole;
import com.wrap.domain.projectmember.enums.ProjectMemberStatus;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(showSql = false)
@Import(ProjectMemberService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProjectMemberConcurrencyTest {

    @Autowired
    private ProjectMemberService service;

    @MockitoSpyBean
    private ProjectRepository projects;

    @Autowired
    private MemberRepository members;

    @Autowired
    private ProjectMemberRepository memberships;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        transaction().executeWithoutResult(status -> {
            memberships.deleteAllInBatch();
            projects.deleteAllInBatch();
            members.deleteAllInBatch();
        });
    }

    @ParameterizedTest(name = "{0} then {1}: last OWNER is preserved")
    @CsvSource({"LEAVE,LEAVE", "DEMOTE,DEMOTE", "LEAVE,DEMOTE", "DEMOTE,LEAVE"})
    void concurrentOwnerDeparturesKeepOneOwner(String firstOperation, String secondOperation)
            throws Exception {
        Fixture fixture = fixture(ProjectMemberRole.OWNER);

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> depart(fixture, fixture.firstMemberId(), fixture.firstMembershipId(), firstOperation),
                () -> depart(fixture, fixture.secondMemberId(), fixture.secondMembershipId(), secondOperation));

        assertThat(rejected).isEqualTo(ErrorCode.LAST_PROJECT_OWNER);
        assertState(fixture.firstMembershipId(),
                firstOperation.equals("LEAVE") ? ProjectMemberRole.OWNER : ProjectMemberRole.MEMBER,
                firstOperation.equals("LEAVE") ? ProjectMemberStatus.LEFT : ProjectMemberStatus.JOINED);
        assertState(fixture.secondMembershipId(), ProjectMemberRole.OWNER, ProjectMemberStatus.JOINED);
        assertOwnerCount(fixture.projectId(), 1);
    }

    @Test
    void waitingRequesterLosesManagementPermissionAfterDemotion() throws Exception {
        Fixture fixture = fixture(ProjectMemberRole.OWNER);

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> changeRole(fixture, fixture.firstMemberId(),
                        fixture.secondMembershipId(), ProjectMemberRole.MEMBER),
                () -> changeRole(fixture, fixture.secondMemberId(),
                        fixture.firstMembershipId(), ProjectMemberRole.MEMBER));

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_OWNER_REQUIRED);
        assertState(fixture.firstMembershipId(), ProjectMemberRole.OWNER, ProjectMemberStatus.JOINED);
        assertState(fixture.secondMembershipId(), ProjectMemberRole.MEMBER, ProjectMemberStatus.JOINED);
        assertOwnerCount(fixture.projectId(), 1);
    }

    @Test
    void waitingRequesterCannotActAfterLeaving() throws Exception {
        Fixture fixture = fixture(ProjectMemberRole.OWNER);

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> service.leaveProject(fixture.secondMemberId(), fixture.projectId()),
                () -> changeRole(fixture, fixture.secondMemberId(),
                        fixture.firstMembershipId(), ProjectMemberRole.MEMBER));

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_ACCESS_DENIED);
        assertState(fixture.firstMembershipId(), ProjectMemberRole.OWNER, ProjectMemberStatus.JOINED);
        assertState(fixture.secondMembershipId(), ProjectMemberRole.OWNER, ProjectMemberStatus.LEFT);
        assertOwnerCount(fixture.projectId(), 1);
    }

    @Test
    void waitingRemovalCannotRemoveNewlyPromotedOwner() throws Exception {
        Fixture fixture = fixture(ProjectMemberRole.MEMBER);

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> changeRole(fixture, fixture.firstMemberId(),
                        fixture.secondMembershipId(), ProjectMemberRole.OWNER),
                () -> service.removeMember(
                        fixture.firstMemberId(), fixture.projectId(), fixture.secondMembershipId()));

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_OWNER_CANNOT_BE_REMOVED);
        assertState(fixture.secondMembershipId(), ProjectMemberRole.OWNER, ProjectMemberStatus.JOINED);
        assertOwnerCount(fixture.projectId(), 2);
    }

    @Test
    void waitingPromotionCannotPromoteRemovedMember() throws Exception {
        Fixture fixture = fixture(ProjectMemberRole.MEMBER);

        ErrorCode rejected = runOverlapping(
                fixture.projectId(),
                () -> service.removeMember(
                        fixture.firstMemberId(), fixture.projectId(), fixture.secondMembershipId()),
                () -> changeRole(fixture, fixture.firstMemberId(),
                        fixture.secondMembershipId(), ProjectMemberRole.OWNER));

        assertThat(rejected).isEqualTo(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        assertState(fixture.secondMembershipId(), ProjectMemberRole.MEMBER, ProjectMemberStatus.LEFT);
        assertOwnerCount(fixture.projectId(), 1);
    }

    private ErrorCode runOverlapping(Long projectId, Runnable firstAction, Runnable secondAction)
            throws Exception {
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

            // 호출 시점만 관찰하고 실제 저장소 쿼리와 DB 잠금은 그대로 실행합니다.
            var repositoryAnswer = mockingDetails(projects).getMockCreationSettings().getDefaultAnswer();
            doAnswer(invocation -> {
                secondLockAttempt.countDown();
                return repositoryAnswer.answer(invocation);
            }).when(projects).findByIdAndDeletedAtIsNullForUpdate(projectId);

            Future<ErrorCode> second = executor.submit(() -> executeRequest(secondAction));
            await(secondLockAttempt);

            // 첫 트랜잭션 커밋 전에는 두 번째 요청이 완료될 수 없습니다.
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

    private Fixture fixture(ProjectMemberRole secondRole) {
        return transaction().execute(status -> {
            Project project = projects.save(Project.create(
                    "Concurrency", null, null, null, null, null, Project.DEFAULT_COLOR));
            Member first = saveMember();
            Member second = saveMember();
            ProjectMember firstMembership = memberships.save(ProjectMember.createOwner(
                    first, project, LocalDateTime.now()));
            ProjectMember secondMembership = memberships.save(ProjectMember.join(
                    second, project, secondRole, LocalDateTime.now()));
            return new Fixture(project.getId(), first.getId(), second.getId(),
                    firstMembership.getId(), secondMembership.getId());
        });
    }

    private Member saveMember() {
        return members.save(Member.builder()
                .email(UUID.randomUUID() + "@example.com")
                .password("encoded-password")
                .nickname("member")
                .build());
    }

    private void depart(Fixture fixture, Long memberId, Long membershipId, String operation) {
        if (operation.equals("LEAVE")) {
            service.leaveProject(memberId, fixture.projectId());
        } else {
            changeRole(fixture, memberId, membershipId, ProjectMemberRole.MEMBER);
        }
    }

    private void changeRole(Fixture fixture, Long memberId, Long targetId, ProjectMemberRole role) {
        ProjectMemberRoleUpdateRequest request = new ProjectMemberRoleUpdateRequest();
        ReflectionTestUtils.setField(request, "role", role);
        service.changeRole(memberId, fixture.projectId(), targetId, request);
    }

    private void assertState(Long membershipId, ProjectMemberRole role, ProjectMemberStatus state) {
        transaction().executeWithoutResult(status -> {
            ProjectMember membership = memberships.findById(membershipId).orElseThrow();
            assertThat(membership.getRole()).isEqualTo(role);
            assertThat(membership.getStatus()).isEqualTo(state);
        });
    }

    private void assertOwnerCount(Long projectId, long expected) {
        transaction().executeWithoutResult(status ->
                assertThat(memberships.countByProjectIdAndRoleAndStatus(
                        projectId, ProjectMemberRole.OWNER, ProjectMemberStatus.JOINED))
                        .isEqualTo(expected));
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

    private record Fixture(Long projectId, Long firstMemberId, Long secondMemberId,
                           Long firstMembershipId, Long secondMembershipId) {
    }
}
