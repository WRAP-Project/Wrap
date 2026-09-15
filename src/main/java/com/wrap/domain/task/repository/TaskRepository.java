package com.wrap.domain.task.repository;

import com.wrap.domain.task.entity.Task;
import com.wrap.domain.task.enums.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    @Query("""
            select t from Task t
            left join fetch t.milestone
            left join fetch t.assignee assignee
            left join fetch assignee.member
            where t.project.id = :projectId
              and (:milestoneId is null or t.milestone.id = :milestoneId)
              and (:status is null or t.status = :status)
              and (:assigneeProjectMemberId is null or assignee.id = :assigneeProjectMemberId)
              and (:deliverable is null or t.deliverable = :deliverable)
              and (:dueFrom is null or t.dueDate >= :dueFrom)
              and (:dueTo is null or t.dueDate <= :dueTo)
            order by case when t.dueDate is null then 1 else 0 end, t.dueDate asc, t.id asc
            """)
    List<Task> findProjectTasks(
            @Param("projectId") Long projectId,
            @Param("milestoneId") Long milestoneId,
            @Param("status") TaskStatus status,
            @Param("assigneeProjectMemberId") Long assigneeProjectMemberId,
            @Param("deliverable") Boolean deliverable,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo
    );

    @Query("""
            select t from Task t
            left join fetch t.assignee assignee
            left join fetch assignee.member
            where t.id = :taskId
              and t.project.id = :projectId
            """)
    Optional<Task> findByIdAndProjectIdWithAssignee(
            @Param("taskId") Long taskId,
            @Param("projectId") Long projectId
    );

    List<Task> findByMilestoneId(Long milestoneId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByProjectIdAndDeliverableTrue(Long projectId);

    @Query("""
            select t from Task t
            left join fetch t.assignee assignee
            left join fetch assignee.member
            where t.project.id = :projectId
            order by case when t.dueDate is null then 1 else 0 end, t.dueDate asc, t.id asc
            """)
    List<Task> findProjectReportTasks(@Param("projectId") Long projectId);

    @Query("""
            select t from Task t
            left join fetch t.assignee assignee
            left join fetch assignee.member
            where t.project.id = :projectId
              and t.dueDate = :dueDate
            order by t.id asc
            """)
    List<Task> findReminderChecklistTasks(
            @Param("projectId") Long projectId,
            @Param("dueDate") LocalDate dueDate
    );

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
