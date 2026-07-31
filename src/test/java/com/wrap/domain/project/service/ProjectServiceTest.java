package com.wrap.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.wrap.domain.member.entity.Member;
import com.wrap.domain.member.repository.MemberRepository;
import com.wrap.domain.project.dto.request.ProjectCreateRequest;
import com.wrap.domain.project.dto.response.ProjectResponse;
import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.enums.ProjectStatus;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.projectmember.entity.ProjectMember;
import com.wrap.domain.projectmember.repository.ProjectMemberRepository;
import com.wrap.global.exception.CustomException;
import com.wrap.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    @DisplayName("프로젝트 생성 성공 - 생성자를 OWNER로 등록한다")
    void create_success() {
        Member member = mock(Member.class);
        ProjectCreateRequest request = createRequest(
                "Wrap",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(projectRepository.save(any(Project.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.create(1L, request);

        assertThat(response.getName()).isEqualTo("Wrap");
        assertThat(response.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);

        ArgumentCaptor<ProjectMember> ownerCaptor =
                ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(ownerCaptor.capture());

        ProjectMember owner = ownerCaptor.getValue();
        assertThat(owner.getMember()).isSameAs(member);
        assertThat(owner.getProject().getName()).isEqualTo("Wrap");
        assertThat(owner.isJoinedOwner()).isTrue();
        assertThat(owner.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 회원을 찾을 수 없다")
    void create_memberNotFound() {
        ProjectCreateRequest request = createRequest(
                "Wrap",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));

        verifyNoInteractions(projectRepository, projectMemberRepository);
    }

    @Test
    @DisplayName("프로젝트 생성 실패 - 시작일이 종료일보다 늦다")
    void create_invalidDateRange() {
        Member member = mock(Member.class);
        ProjectCreateRequest request = createRequest(
                "Wrap",
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 7, 1)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> projectService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(
                        ((CustomException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.INVALID_DATE_RANGE));

        verifyNoInteractions(projectRepository, projectMemberRepository);
    }

    private ProjectCreateRequest createRequest(
            String name,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ProjectCreateRequest request = new ProjectCreateRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "description", "프로젝트 설명");
        ReflectionTestUtils.setField(request, "goal", "프로젝트 목표");
        ReflectionTestUtils.setField(request, "successCriteria", "성공 기준");
        ReflectionTestUtils.setField(request, "startDate", startDate);
        ReflectionTestUtils.setField(request, "endDate", endDate);
        return request;
    }
}
