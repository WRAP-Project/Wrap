package com.wrap.domain.task.repository;

import com.wrap.domain.task.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByMilestoneId(Long milestoneId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByProjectIdAndDeliverableTrue(Long projectId);
}
