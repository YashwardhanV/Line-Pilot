package com.yashwardhanv.linepilot.dto;

public record QueueSummaryResponse(
        Long id,
        String code,
        String name,
        String location,
        boolean open,
        long waitingCount,
        long estimatedWaitMinutes
) {
}
