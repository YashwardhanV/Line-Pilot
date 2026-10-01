package com.yashwardhanv.linepilot.controller;

import com.yashwardhanv.linepilot.dto.HistoryPageResponse;
import com.yashwardhanv.linepilot.dto.TokenResponse;
import com.yashwardhanv.linepilot.entity.TokenStatus;
import com.yashwardhanv.linepilot.service.QueueEventStream;
import com.yashwardhanv.linepilot.service.QueueService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/staff")
@SecurityRequirement(name = "basicAuth")
public class StaffQueueController {

    private final QueueService queueService;
    private final QueueEventStream eventStream;

    public StaffQueueController(QueueService queueService, QueueEventStream eventStream) {
        this.queueService = queueService;
        this.eventStream = eventStream;
    }

    @PostMapping("/queues/{queueId}/call-next")
    public TokenResponse callNext(@PathVariable Long queueId, Principal principal) {
        return notifyBoards(queueService.callNext(queueId, principal.getName()));
    }

    @PostMapping("/tokens/{tokenId}/start")
    public TokenResponse start(@PathVariable Long tokenId, Principal principal) {
        return notifyBoards(queueService.startServing(tokenId, principal.getName()));
    }

    @PostMapping("/tokens/{tokenId}/complete")
    public TokenResponse complete(@PathVariable Long tokenId, Principal principal) {
        return notifyBoards(queueService.complete(tokenId, principal.getName()));
    }

    @PostMapping("/tokens/{tokenId}/skip")
    public TokenResponse skip(@PathVariable Long tokenId, Principal principal) {
        return notifyBoards(queueService.skip(tokenId, principal.getName()));
    }

    @GetMapping("/queues/{queueId}/history")
    public HistoryPageResponse history(@PathVariable Long queueId,
                                       @RequestParam(required = false) TokenStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return queueService.history(queueId, status, page, size);
    }

    // The service call above has already committed, so live boards never see a change that rolls back.
    private TokenResponse notifyBoards(TokenResponse token) {
        eventStream.broadcast(token.queueId(), queueService.snapshot(token.queueId()));
        return token;
    }
}
