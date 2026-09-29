package com.wrap.domain.project.service;

public final class ProjectProgressCalculator {

    private ProjectProgressCalculator() {
    }

    public static int calculate(long doneCount, long totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return (int) Math.round(doneCount * 100.0 / totalCount);
    }
}
