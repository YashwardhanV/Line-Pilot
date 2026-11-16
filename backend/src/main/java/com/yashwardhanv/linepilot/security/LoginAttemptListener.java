package com.yashwardhanv.linepilot.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/** Counts wrong passwords per IP; a successful sign-in clears the count. */
@Component
public class LoginAttemptListener {

    private final FixedWindowRateLimiter failedLoginLimiter;

    public LoginAttemptListener(@Qualifier("failedLoginLimiter") FixedWindowRateLimiter failedLoginLimiter) {
        this.failedLoginLimiter = failedLoginLimiter;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String ip = ipOf(event.getAuthentication());
        if (ip != null) {
            failedLoginLimiter.tryAcquire(ip);
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String ip = ipOf(event.getAuthentication());
        if (ip != null) {
            failedLoginLimiter.reset(ip);
        }
    }

    private String ipOf(Authentication authentication) {
        return authentication.getDetails() instanceof WebAuthenticationDetails details
                ? details.getRemoteAddress()
                : null;
    }
}
