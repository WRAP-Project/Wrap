package com.wrap.domain.schedule.entity;

import com.wrap.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "schedule_check",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_check_schedule_member",
                columnNames = {"schedule_id", "member_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    public static ScheduleCheck create(Schedule schedule, Member member, LocalDateTime checkedAt) {
        ScheduleCheck scheduleCheck = new ScheduleCheck();
        scheduleCheck.schedule = schedule;
        scheduleCheck.member = member;
        scheduleCheck.checkedAt = checkedAt;
        return scheduleCheck;
    }
}
