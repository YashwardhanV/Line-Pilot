package com.yashwardhanv.linepilot.dto;

import com.yashwardhanv.linepilot.entity.TokenStatus;

import java.time.Instant;

public record QueueTokenView(
        Long id,
        String displayNumber,
        TokenStatus status,
        String claimedBy,
        Instant joinedAt,
        Instant updatedAt
) {
}
