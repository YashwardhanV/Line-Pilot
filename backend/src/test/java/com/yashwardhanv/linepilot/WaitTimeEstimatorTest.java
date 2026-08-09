package com.yashwardhanv.linepilot;

import com.yashwardhanv.linepilot.service.WaitTimeEstimator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaitTimeEstimatorTest {

    private final WaitTimeEstimator estimator = new WaitTimeEstimator();

    @Test
    void usesConfiguredFallbackWhenNoCompletedServicesExist() {
        assertThat(estimator.estimateMinutes(3, List.of(), 6)).isEqualTo(18);
    }

    @Test
    void usesRoundedMovingAverageOfRecentServiceDurations() {
        List<Duration> samples = List.of(
                Duration.ofMinutes(4),
                Duration.ofMinutes(5),
                Duration.ofMinutes(9));

        assertThat(estimator.averageServiceMinutes(samples, 6)).isEqualTo(6);
        assertThat(estimator.estimateMinutes(2, samples, 6)).isEqualTo(12);
    }

    @Test
    void returnsZeroWhenNoOneIsAhead() {
        assertThat(estimator.estimateMinutes(0, List.of(Duration.ofMinutes(5)), 6)).isZero();
    }
}
