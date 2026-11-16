package com.yashwardhanv.linepilot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    /** Anonymous "join queue" requests per IP, so one script cannot fill a real queue. */
    @Bean
    FixedWindowRateLimiter joinRateLimiter(@Value("${app.rate-limit.joins-per-ip}") int joinsPerIp,
                                           Clock clock) {
        return new FixedWindowRateLimiter(joinsPerIp, Duration.ofMinutes(10), clock);
    }

    /** Failed staff sign-ins per IP before that IP is blocked for 15 minutes. */
    @Bean
    FixedWindowRateLimiter failedLoginLimiter(@Value("${app.rate-limit.failed-logins-per-ip}") int failures,
                                              Clock clock) {
        return new FixedWindowRateLimiter(failures, Duration.ofMinutes(15), clock);
    }
}
