package com.yashwardhanv.linepilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinQueueRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 100, message = "Customer name must not exceed 100 characters")
        String customerName,

        // Optional. International format so it works with SMS/WhatsApp providers.
        @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "Phone must be in international format, for example +919876543210")
        String customerPhone
) {
}
