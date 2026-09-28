package com.wrap.domain.milestone.service;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.milestone.dto.request.MilestoneCreateRequest;
import com.wrap.domain.milestone.dto.request.MilestoneUpdateRequest;
import com.wrap.domain.milestone.dto.response.MilestoneResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

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
}
