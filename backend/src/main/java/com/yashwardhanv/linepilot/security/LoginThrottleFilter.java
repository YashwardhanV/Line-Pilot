package com.yashwardhanv.linepilot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Rejects sign-in attempts from an IP that has had too many wrong passwords recently. */
public class LoginThrottleFilter extends OncePerRequestFilter {

    private final FixedWindowRateLimiter failedLoginLimiter;

    public LoginThrottleFilter(FixedWindowRateLimiter failedLoginLimiter) {
        this.failedLoginLimiter = failedLoginLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Basic ")
                && failedLoginLimiter.isLimited(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"https://linepilot.local/problems/429","title":"Too many requests","status":429,\
                    "detail":"Too many failed sign-in attempts. Try again in %d minutes."}"""
                    .formatted(failedLoginLimiter.window().toMinutes()));
            return;
        }
        chain.doFilter(request, response);
    }
}
