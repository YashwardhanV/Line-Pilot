package com.yashwardhanv.linepilot.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

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

    public long estimateMinutes(long peopleAhead, List<Duration> recentDurations, int fallbackMinutes) {
        if (peopleAhead <= 0) {
            return 0;
        }
        return peopleAhead * averageServiceMinutes(recentDurations, fallbackMinutes);
    }
}
