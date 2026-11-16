package com.yashwardhanv.linepilot.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts events per key (an IP address) in fixed time windows. Kept in memory, which is fine for
 * the single backend instance LinePilot runs as.
 */
public class FixedWindowRateLimiter {

    private record Window(Instant start, int count) {
    }

    private final int limit;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int limit, Duration window, Clock clock) {
        this.limit = limit;
        this.window = window;
        this.clock = clock;
    }

    /** Records one event and returns false if the key is now over its limit. */
    public boolean tryAcquire(String key) {
        Instant now = clock.instant();
        Window current = windows.compute(key, (ignored, existing) ->
                existing == null || expired(existing, now)
                        ? new Window(now, 1)
                        : new Window(existing.start(), existing.count() + 1));
        return current.count() <= limit;
    }

    /** True if the key has already used up its limit in the current window. */
    public boolean isLimited(String key) {
        Window current = windows.get(key);
        return current != null && !expired(current, clock.instant()) && current.count() >= limit;
    }

    public void reset(String key) {
        windows.remove(key);
    }

    public void clear() {
        windows.clear();
    }

    public void evictExpired() {
        Instant now = clock.instant();
        windows.values().removeIf(existing -> expired(existing, now));
    }

    public Duration window() {
        return window;
    }

    private boolean expired(Window existing, Instant now) {
        return !now.isBefore(existing.start().plus(window));
    }
}
