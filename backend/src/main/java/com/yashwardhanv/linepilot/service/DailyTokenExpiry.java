package com.yashwardhanv.linepilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Closes tokens from earlier days. Runs just after local midnight, and once at startup in case the
 * server was down at midnight.
 */
@Component
public class DailyTokenExpiry {

    private static final Logger log = LoggerFactory.getLogger(DailyTokenExpiry.class);

    private final QueueService queueService;
    private final QueueEventStream eventStream;

    public DailyTokenExpiry(QueueService queueService, QueueEventStream eventStream) {
        this.queueService = queueService;
        this.eventStream = eventStream;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        expire();
    }

    @Scheduled(cron = "0 1 0 * * *", zone = "${app.time-zone}")
    public void expire() {
        int expired = queueService.expireTokensFromEarlierDays();
        if (expired == 0) {
            return;
        }
        log.info("Expired {} unserved tokens from earlier days", expired);
        for (Long queueId : queueService.queueIds()) {
            eventStream.broadcast(queueId, queueService.snapshot(queueId));
        }
    }
}
