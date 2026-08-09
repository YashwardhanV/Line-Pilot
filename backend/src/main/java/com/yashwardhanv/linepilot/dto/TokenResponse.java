package com.yashwardhanv.linepilot.dto;

import com.yashwardhanv.linepilot.entity.TokenStatus;

import java.time.Instant;
import java.util.UUID;

public record TokenResponse(
        Long id,
        UUID publicId,
        Long queueId,
        String queueName,
        String displayNumber,
        String customerName,
        TokenStatus status,
        long peopleAhead,
        long estimatedWaitMinutes,
        String claimedBy,
        Instant joinedAt,
        Instant updatedAt
) {
}
