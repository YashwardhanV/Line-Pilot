package com.yashwardhanv.linepilot.dto;

import java.util.List;

public record HistoryPageResponse(
        List<HistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
