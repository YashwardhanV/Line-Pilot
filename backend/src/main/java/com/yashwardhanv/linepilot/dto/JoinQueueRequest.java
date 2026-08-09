package com.yashwardhanv.linepilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinQueueRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 100, message = "Customer name must not exceed 100 characters")
        String customerName
) {
}
