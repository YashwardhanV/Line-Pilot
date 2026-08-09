package com.yashwardhanv.linepilot.controller;

import com.yashwardhanv.linepilot.dto.JoinQueueRequest;
import com.yashwardhanv.linepilot.dto.QueueSnapshotResponse;
import com.yashwardhanv.linepilot.dto.QueueSummaryResponse;
import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.service.QueueCommandService;
import com.yashwardhanv.linepilot.service.QueueEventStream;
import com.yashwardhanv.linepilot.service.QueueQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PublicQueueController {

    private final QueueQueryService queryService;
    private final QueueCommandService commandService;
    private final QueueEventStream eventStream;

    public PublicQueueController(QueueQueryService queryService,
                                 QueueCommandService commandService,
                                 QueueEventStream eventStream) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.eventStream = eventStream;
    }

    @GetMapping("/queues")
    public List<QueueSummaryResponse> listQueues() {
        return queryService.listQueues();
    }

    @GetMapping("/queues/{queueId}")
    public QueueSnapshotResponse queue(@PathVariable Long queueId) {
        return queryService.snapshot(queueId);
    }

    @PostMapping("/queues/{queueId}/tokens")
    public ResponseEntity<TokenResponse> join(@PathVariable Long queueId,
                                              @Valid @RequestBody JoinQueueRequest request) {
        TokenResponse token = commandService.joinQueue(queueId, request.customerName());
        return ResponseEntity.created(URI.create("/api/tokens/" + token.publicId())).body(token);
    }

    @GetMapping("/tokens/{publicId}")
    public TokenResponse token(@PathVariable UUID publicId) {
        return queryService.findPublicToken(publicId);
    }

    @PostMapping("/tokens/{publicId}/cancel")
    public TokenResponse cancel(@PathVariable UUID publicId) {
        return commandService.cancel(publicId);
    }

    @GetMapping(value = "/queues/{queueId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> events(@PathVariable Long queueId) {
        SseEmitter emitter = eventStream.subscribe(queueId, queryService.snapshot(queueId));
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }
}
