package com.yashwardhanv.linepilot.controller;

import com.yashwardhanv.linepilot.dto.QueueRequest;
import com.yashwardhanv.linepilot.dto.QueueSummaryResponse;
import com.yashwardhanv.linepilot.service.QueueCommandService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/queues")
@SecurityRequirement(name = "basicAuth")
public class AdminQueueController {

    private final QueueCommandService commandService;

    public AdminQueueController(QueueCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<QueueSummaryResponse> create(@Valid @RequestBody QueueRequest request) {
        QueueSummaryResponse queue = commandService.createQueue(request);
        return ResponseEntity.created(URI.create("/api/queues/" + queue.id())).body(queue);
    }

    @PutMapping("/{queueId}")
    public QueueSummaryResponse update(@PathVariable Long queueId,
                                       @Valid @RequestBody QueueRequest request) {
        return commandService.updateQueue(queueId, request);
    }
}
