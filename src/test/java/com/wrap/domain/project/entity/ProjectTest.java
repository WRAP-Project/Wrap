package com.wrap.domain.project.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wrap.domain.project.enums.ProjectStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ProjectTest {

    @Test
    void 프로젝트를_생성하면_진행_중_상태가_된다() {
        Project project = createProject();

        assertEquals("Wrap", project.getName());
        assertEquals(ProjectStatus.IN_PROGRESS, project.getStatus());
        assertNull(project.getCompletedAt());
        assertNull(project.getDeletedAt());
    }

    @Test
    void 선택_문자열이_공백이면_null로_정규화한다() {
        Project project = Project.create(
                "Wrap",
                "  ",
                "",
                null,
                null,
                null
        );

        assertNull(project.getDescription());
        assertNull(project.getGoal());
        assertNull(project.getSuccessCriteria());
    }

    @Test
    void 시작일이_종료일보다_늦으면_생성할_수_없다() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Project.create(
                        "Wrap",
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 7, 31)
                )
        );
    }

    @Test
    void 잘못된_날짜로_수정하면_기존_값을_유지한다() {
        Project project = createProject();

        assertThrows(
                IllegalArgumentException.class,
                () -> project.update(
                        "변경된 이름",
                        "변경된 설명",
                        null,
                        null,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 8, 31)
                )
        );

        assertEquals("Wrap", project.getName());
        assertEquals("프로젝트 설명", project.getDescription());
        assertEquals(LocalDate.of(2026, 7, 1), project.getStartDate());
        assertEquals(LocalDate.of(2026, 8, 31), project.getEndDate());
    }

    @Test
    void 프로젝트를_완료하고_다시_진행할_수_있다() {
        Project project = createProject();
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 26, 12, 0);

        project.complete(completedAt);

        assertTrue(project.isCompleted());
        assertEquals(completedAt, project.getCompletedAt());

        project.reopen();

        assertFalse(project.isCompleted());
        assertEquals(ProjectStatus.IN_PROGRESS, project.getStatus());
        assertNull(project.getCompletedAt());
    }

    @Test
    void 프로젝트를_soft_delete할_수_있다() {
        Project project = createProject();
        LocalDateTime deletedAt = LocalDateTime.of(2026, 7, 26, 13, 0);

        project.softDelete(deletedAt);

        assertTrue(project.isDeleted());
        assertEquals(deletedAt, project.getDeletedAt());
    }

    @Test
    void 완료된_프로젝트를_다시_완료할_수_없다() {
        Project project = createProject();
        project.complete(LocalDateTime.of(2026, 7, 26, 12, 0));

        assertThrows(
                IllegalStateException.class,
                () -> project.complete(LocalDateTime.of(2026, 7, 26, 13, 0))
        );
    }

    private Project createProject() {
        return Project.create(
                "  Wrap  ",
                "프로젝트 설명",
                "프로젝트 목표",
                "성공 기준",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );
    }
}
