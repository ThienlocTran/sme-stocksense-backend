package com.smartflow.smestocksensebackend.dto.forecast;

import java.time.LocalDate;

public record SeedHistoryResponse(
        String source,
        int seriesSeeded,
        int rowsInserted,
        LocalDate historyStart,
        LocalDate historyEnd) {
}
