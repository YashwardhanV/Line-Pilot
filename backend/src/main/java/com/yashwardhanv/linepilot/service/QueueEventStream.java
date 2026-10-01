package com.yashwardhanv.linepilot.service;

import com.yashwardhanv.linepilot.dto.QueueSnapshotResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class QueueEventStream {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final long timeout;

    public QueueEventStream(@Value("${app.sse-timeout}") long timeout) {
        this.timeout = timeout;
    }

    public SseEmitter subscribe(Long queueId, QueueSnapshotResponse initialSnapshot) {
        SseEmitter emitter = new SseEmitter(timeout);
        CopyOnWriteArrayList<SseEmitter> queueEmitters =
                emitters.computeIfAbsent(queueId, ignored -> new CopyOnWriteArrayList<>());
        queueEmitters.add(emitter);
        Runnable cleanup = () -> queueEmitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());

        try {
            send(emitter, initialSnapshot);
        } catch (IOException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void broadcast(Long queueId, QueueSnapshotResponse snapshot) {
        CopyOnWriteArrayList<SseEmitter> queueEmitters = emitters.get(queueId);
        if (queueEmitters == null) {
            return;
        }
        for (SseEmitter emitter : queueEmitters) {
            try {
                send(emitter, snapshot);
            } catch (IOException | IllegalStateException exception) {
                queueEmitters.remove(emitter);
                emitter.complete();
            }
        }
    }

    private void send(SseEmitter emitter, QueueSnapshotResponse snapshot) throws IOException {
        emitter.send(SseEmitter.event()
                .name("queue.updated")
                .data(snapshot));
    }
}
