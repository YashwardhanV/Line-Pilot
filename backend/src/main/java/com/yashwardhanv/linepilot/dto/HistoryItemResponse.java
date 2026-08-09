package com.yashwardhanv.linepilot.dto;

import com.yashwardhanv.linepilot.entity.TokenStatus;

import java.time.Instant;

public record HistoryItemResponse(
        Long id,
        String displayNumber,
        String customerName,
        TokenStatus status,
        String claimedBy,
        Instant joinedAt,
        Instant calledAt,
        Instant servingAt,
        Instant completedAt
) {
}
