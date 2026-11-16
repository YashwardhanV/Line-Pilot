package com.yashwardhanv.linepilot.entity;

public enum TokenStatus {
    WAITING,
    CALLED,
    SERVING,
    COMPLETED,
    SKIPPED,
    CANCELLED,
    EXPIRED // left over from an earlier day
}
