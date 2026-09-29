package com.wrap.domain.milestone.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.milestone.dto.request.MilestoneCreateRequest;
import com.wrap.domain.milestone.dto.request.MilestoneUpdateRequest;
import com.wrap.domain.milestone.dto.response.MilestoneResponse;
import com.wrap.domain.milestone.entity.Milestone;
import com.wrap.domain.milestone.enums.MilestoneStatus;
import com.wrap.domain.milestone.repository.MilestoneRepository;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.TaskRepository;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class MilestoneServiceIntegrationTest {

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private TaskRepository taskRepository;

    private Long memberId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
                .email("milestone-%d@wrap.com".formatted(System.nanoTime()))
                .password("encoded")
                .nickname("성찬")
                .build());
        Project project = projectRepository.save(Project.create(
                "Wrap",
                null,
                null,
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 30),
                "#CDEA6F"
        ));
        ProjectMember projectMember = projectMemberRepository.save(
                ProjectMember.createOwner(member, project, LocalDateTime.now())
        );

        memberId = member.getId();
        projectId = project.getId();
        assertThat(projectMember.getId()).isNotNull();
    }

    @Test
    void createdMilestoneSurvivesReload() {
        MilestoneResponse created = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("중간 발표 자료 완성", LocalDate.of(2026, 8, 20))
        );

        List<MilestoneResponse> reloaded = milestoneService.findProjectMilestones(memberId, projectId);

        assertThat(reloaded).hasSize(1);
        assertThat(reloaded.get(0).id()).isEqualTo(created.id());
        assertThat(reloaded.get(0).title()).isEqualTo("중간 발표 자료 완성");
        assertThat(reloaded.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void milestonesAreOrderedByDueDate() {
        milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("최종 발표", LocalDate.of(2026, 9, 10))
        );
        milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("중간 발표", LocalDate.of(2026, 8, 20))
        );

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .extracting(MilestoneResponse::title)
                .containsExactly("중간 발표", "최종 발표");
    }

    @Test
    void updateAndDeletePersist() {
        MilestoneResponse created = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("중간 발표", LocalDate.of(2026, 8, 20))
        );

        milestoneService.update(
                memberId,
                projectId,
                created.id(),
                new MilestoneUpdateRequest(null, null, LocalDate.of(2026, 8, 25))
        );

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .singleElement()
                .satisfies(milestone -> {
                    assertThat(milestone.title()).isEqualTo("중간 발표");
                    assertThat(milestone.dueDate()).isEqualTo(LocalDate.of(2026, 8, 25));
                });

        milestoneService.delete(memberId, projectId, created.id());

        assertThat(milestoneService.findProjectMilestones(memberId, projectId)).isEmpty();
    }

    @Test
    void taskCountsAreAggregatedPerMilestone() {
        MilestoneResponse partial = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("1차 배포", LocalDate.of(2026, 8, 20))
        );
        MilestoneResponse allDone = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("QA 완료", LocalDate.of(2026, 8, 25))
        );
        MilestoneResponse empty = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("회고", LocalDate.of(2026, 8, 28))
        );

        Project project = projectRepository.findById(projectId).orElseThrow();
        Milestone partialMilestone = milestoneRepository.findById(partial.id()).orElseThrow();
        Milestone allDoneMilestone = milestoneRepository.findById(allDone.id()).orElseThrow();

        saveTask(project, partialMilestone, TaskStatus.DONE);
        saveTask(project, partialMilestone, TaskStatus.IN_PROGRESS);
        saveTask(project, partialMilestone, TaskStatus.TODO);
        saveTask(project, allDoneMilestone, TaskStatus.DONE);
        saveTask(project, allDoneMilestone, TaskStatus.DONE);
        saveTask(project, null, TaskStatus.DONE);

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .extracting(
                        MilestoneResponse::id,
                        MilestoneResponse::totalTaskCount,
                        MilestoneResponse::doneTaskCount
                )
                .containsExactly(
                        tuple(partial.id(), 3, 1),
                        tuple(allDone.id(), 2, 2),
                        tuple(empty.id(), 0, 0)
                );
    }

    @Test
    void lastTaskCompletionPromotesMilestoneToDone() {
        MilestoneResponse created = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("1차 배포", LocalDate.of(2026, 8, 20))
        );
        Project project = projectRepository.findById(projectId).orElseThrow();
        Milestone milestone = milestoneRepository.findById(created.id()).orElseThrow();
        saveTask(project, milestone, TaskStatus.DONE);
        Task lastTask = saveTask(project, milestone, TaskStatus.IN_PROGRESS);

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .singleElement()
                .satisfies(found -> assertThat(found.status()).isEqualTo(MilestoneStatus.IN_PROGRESS));

        updateTaskStatus(lastTask, TaskStatus.DONE);

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.status()).isEqualTo(MilestoneStatus.DONE);
                    assertThat(found.totalTaskCount()).isEqualTo(2);
                    assertThat(found.doneTaskCount()).isEqualTo(2);
                });
        assertThat(milestoneRepository.findById(created.id()).orElseThrow().getStatus())
                .isEqualTo(MilestoneStatus.DONE);
    }

    @Test
    void reopeningTaskDoesNotDemoteCompletedMilestone() {
        MilestoneResponse created = milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("1차 배포", LocalDate.of(2026, 8, 20))
        );
        Project project = projectRepository.findById(projectId).orElseThrow();
        Milestone milestone = milestoneRepository.findById(created.id()).orElseThrow();
        Task task = saveTask(project, milestone, TaskStatus.DONE);

        milestoneService.findProjectMilestones(memberId, projectId);
        updateTaskStatus(task, TaskStatus.IN_PROGRESS);

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.status()).isEqualTo(MilestoneStatus.DONE);
                    assertThat(found.doneTaskCount()).isZero();
                });
    }

    @Test
    void milestoneWithoutTasksIsNotPromoted() {
        milestoneService.create(
                memberId,
                projectId,
                new MilestoneCreateRequest("회고", LocalDate.of(2026, 8, 28))
        );

        assertThat(milestoneService.findProjectMilestones(memberId, projectId))
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.status()).isEqualTo(MilestoneStatus.IN_PROGRESS);
                    assertThat(found.totalTaskCount()).isZero();
                });
    }

    private void updateTaskStatus(Task task, TaskStatus status) {
        Task stored = taskRepository.findById(task.getId()).orElseThrow();
        stored.updateStatus(status);
        taskRepository.save(stored);
    }

    private Task saveTask(Project project, Milestone milestone, TaskStatus status) {
        Task task;
        try {
            Constructor<Task> constructor = Task.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            task = constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity.", exception);
        }

        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "milestone", milestone);
        ReflectionTestUtils.setField(task, "title", "할 일");
        task.updateStatus(status);
        return taskRepository.save(task);
    }
}
