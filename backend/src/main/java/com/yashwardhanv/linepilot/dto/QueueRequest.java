package com.yashwardhanv.linepilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QueueRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9_-]{2,30}") String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 160) String location,
        @NotBlank @Pattern(regexp = "[A-Z0-9]{1,5}") String tokenPrefix,
        boolean open,
        @Min(1) @Max(240) int defaultServiceMinutes
) {
}
