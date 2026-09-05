package com.wrap.domain.task.repository;

import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByMilestoneId(Long milestoneId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByProjectIdAndDeliverableTrue(Long projectId);

    @Query("""
            select t from Task t
            join fetch t.project
            left join fetch t.assignee assignee
            left join fetch assignee.member
            where t.project.id = :projectId
              and t.status <> :excludedStatus
              and (:status is null or t.status = :status)
              and (:assigneeProjectMemberId is null or assignee.id = :assigneeProjectMemberId)
              and (:dueFrom is null or t.dueDate >= :dueFrom)
              and (:dueTo is null or t.dueDate <= :dueTo)
            order by t.dueDate asc, t.id asc
            """)
    List<Task> findCalendarRiskChecks(
            @Param("projectId") Long projectId,
            @Param("status") TaskStatus status,
            @Param("assigneeProjectMemberId") Long assigneeProjectMemberId,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("excludedStatus") TaskStatus excludedStatus
    );
}
