package com.yashwardhanv.linepilot.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Drops finished rate-limit windows so the in-memory maps do not grow forever. */
@Component
public class RateLimitCleanup {

    private final List<FixedWindowRateLimiter> limiters;

    public RateLimitCleanup(List<FixedWindowRateLimiter> limiters) {
        this.limiters = limiters;
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void evictExpiredWindows() {
        limiters.forEach(FixedWindowRateLimiter::evictExpired);
    }
}
