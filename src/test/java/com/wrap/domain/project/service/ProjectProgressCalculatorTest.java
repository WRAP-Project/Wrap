package com.wrap.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProjectProgressCalculatorTest {

    @Test
    void returnsZeroWhenThereAreNoTasks() {
        assertThat(ProjectProgressCalculator.calculate(0, 0)).isZero();
    }

    @Test
    void roundsProgressToNearestInteger() {
        assertThat(ProjectProgressCalculator.calculate(2, 3)).isEqualTo(67);
    }

    @Test
    void returnsOneHundredWhenAllTasksAreDone() {
        assertThat(ProjectProgressCalculator.calculate(4, 4)).isEqualTo(100);
    }
}
