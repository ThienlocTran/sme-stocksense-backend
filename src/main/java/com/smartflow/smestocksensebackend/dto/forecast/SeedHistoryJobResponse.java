package com.smartflow.smestocksensebackend.dto.forecast;

import java.time.LocalDateTime;
import java.util.UUID;

public record SeedHistoryJobResponse(
        UUID jobId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer rowsInserted,
        Integer seriesSeeded,
        String errorMessage) {
}
