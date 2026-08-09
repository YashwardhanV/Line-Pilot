package com.yashwardhanv.linepilot.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class QueueChangedListener {

    private final QueueQueryService queryService;
    private final QueueEventStream eventStream;

    public QueueChangedListener(QueueQueryService queryService, QueueEventStream eventStream) {
        this.queryService = queryService;
        this.eventStream = eventStream;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueueChanged(QueueChangedEvent event) {
        eventStream.broadcast(event.queueId(), queryService.snapshot(event.queueId()));
    }
}
