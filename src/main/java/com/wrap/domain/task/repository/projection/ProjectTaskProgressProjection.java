package com.wrap.domain.task.repository.projection;

public interface ProjectTaskProgressProjection {

    Long getProjectId();

    long getTotalCount();

    long getDoneCount();
}
