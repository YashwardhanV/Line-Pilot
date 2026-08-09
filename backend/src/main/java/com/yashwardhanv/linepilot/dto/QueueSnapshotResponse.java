package com.yashwardhanv.linepilot.dto;

import java.time.Instant;
import java.util.List;

public record QueueSnapshotResponse(
        Long queueId,
        String queueName,
        String location,
        boolean open,
        long waitingCount,
        long estimatedWaitMinutes,
        Instant generatedAt,
        List<QueueTokenView> tokens
) {
}
