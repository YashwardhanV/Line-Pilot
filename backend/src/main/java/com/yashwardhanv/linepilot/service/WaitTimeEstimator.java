package com.yashwardhanv.linepilot.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Simple, explainable estimate: people ahead are shared between the staff currently working the
 * queue, and each "round" takes the average of the last ten service times.
 */
@Component
public class WaitTimeEstimator {

    public long averageServiceMinutes(List<Duration> recentDurations, int fallbackMinutes) {
        if (recentDurations.isEmpty()) {
            return fallbackMinutes;
        }

        double averageSeconds = recentDurations.stream()
                .mapToLong(Duration::toSeconds)
                .average()
                .orElse(fallbackMinutes * 60.0);
        return Math.max(1, Math.round(averageSeconds / 60.0));
    }

    public long estimateMinutes(long peopleAhead, long averageServiceMinutes, long activeStaff) {
        if (peopleAhead <= 0) {
            return 0;
        }
        long staff = Math.max(1, activeStaff);
        long rounds = (peopleAhead + staff - 1) / staff; // ceil(peopleAhead / staff)
        return rounds * averageServiceMinutes;
    }
}
