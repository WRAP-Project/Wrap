package com.wrap.domain.task.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wrap.domain.project.entity.Project;
import com.wrap.domain.project.repository.ProjectRepository;
import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskPriority;
import com.wrap.domain.task.enums.TaskStatus;
import com.wrap.domain.task.repository.projection.ProjectTaskProgressProjection;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void aggregatesTotalAndDoneTaskCountsForMultipleProjects() {
        Project firstProject = project("First");
        Project secondProject = project("Second");
        Project projectWithoutTasks = project("Empty");

        task(firstProject, TaskStatus.DONE);
        task(firstProject, TaskStatus.DONE);
        task(firstProject, TaskStatus.IN_PROGRESS);
        task(secondProject, TaskStatus.TODO);
        task(secondProject, TaskStatus.HOLD);

        List<ProjectTaskProgressProjection> results =
                taskRepository.findProjectProgressCounts(
                        List.of(
                                firstProject.getId(),
                                secondProject.getId(),
                                projectWithoutTasks.getId()
                        ),
                        TaskStatus.DONE
                );

        Map<Long, ProjectTaskProgressProjection> byProjectId = results.stream()
                .collect(Collectors.toMap(
                        ProjectTaskProgressProjection::getProjectId,
                        Function.identity()
                ));

        assertThat(byProjectId).hasSize(2);
        assertThat(byProjectId.get(firstProject.getId()).getTotalCount()).isEqualTo(3);
        assertThat(byProjectId.get(firstProject.getId()).getDoneCount()).isEqualTo(2);
        assertThat(byProjectId.get(secondProject.getId()).getTotalCount()).isEqualTo(2);
        assertThat(byProjectId.get(secondProject.getId()).getDoneCount()).isZero();
        assertThat(byProjectId).doesNotContainKey(projectWithoutTasks.getId());
    }

    private Project project(String name) {
        return projectRepository.save(Project.create(
                name,
                null,
                null,
                null,
                null,
                null,
                Project.DEFAULT_COLOR
        ));
    }

    private Task task(Project project, TaskStatus status) {
        Task task = instantiate(Task.class);
        ReflectionTestUtils.setField(task, "project", project);
        ReflectionTestUtils.setField(task, "title", "Task");
        ReflectionTestUtils.setField(task, "status", status);
        ReflectionTestUtils.setField(task, "priority", TaskPriority.MEDIUM);
        return taskRepository.save(task);
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate test entity.", exception);
        }
    }
}
