package com.yashwardhanv.linepilot.service;

import java.time.Instant;

public record QueueChangedEvent(Long queueId, Instant occurredAt) {
}
