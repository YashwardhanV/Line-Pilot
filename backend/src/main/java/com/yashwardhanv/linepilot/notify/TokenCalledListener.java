package com.yashwardhanv.linepilot.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the "your turn" message after the call-next transaction commits (so a rolled-back call
 * never texts anyone), on a background thread (so a slow provider never slows down staff).
 */
@Component
public class TokenCalledListener {

    private static final Logger log = LoggerFactory.getLogger(TokenCalledListener.class);

    private final CustomerNotifier notifier;

    public TokenCalledListener(CustomerNotifier notifier) {
        this.notifier = notifier;
    }

    @Async
    @TransactionalEventListener
    public void onTokenCalled(TokenCalledEvent event) {
        try {
            notifier.send(event.phone(), event.message());
        } catch (RuntimeException exception) {
            // The customer can still see the call on the live board; don't fail anything else.
            log.warn("Could not notify customer for token {}: {}", event.displayNumber(), exception.getMessage());
        }
    }
}
