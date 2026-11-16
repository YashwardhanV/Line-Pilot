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
        assertThat(estimator.averageServiceMinutes(List.of(), 6)).isEqualTo(6);
    }

    @Test
    void usesRoundedMovingAverageOfRecentServiceDurations() {
        List<Duration> samples = List.of(
                Duration.ofMinutes(4),
                Duration.ofMinutes(5),
                Duration.ofMinutes(9));

        assertThat(estimator.averageServiceMinutes(samples, 6)).isEqualTo(6);
    }

    @Test
    void oneStaffMemberServesEveryoneAheadInTurn() {
        assertThat(estimator.estimateMinutes(3, 6, 1)).isEqualTo(18);
    }

    @Test
    void peopleAheadAreSharedBetweenActiveStaff() {
        // 3 people ahead, 2 staff: two rounds of service, not three.
        assertThat(estimator.estimateMinutes(3, 6, 2)).isEqualTo(12);
        assertThat(estimator.estimateMinutes(4, 6, 2)).isEqualTo(12);
    }

    @Test
    void treatsNoActiveStaffAsOne() {
        assertThat(estimator.estimateMinutes(2, 5, 0)).isEqualTo(10);
    }

    @Test
    void returnsZeroWhenNoOneIsAhead() {
        assertThat(estimator.estimateMinutes(0, 5, 1)).isZero();
    }
}
