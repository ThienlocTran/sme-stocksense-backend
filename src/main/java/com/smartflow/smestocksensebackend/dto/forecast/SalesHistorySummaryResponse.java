package com.smartflow.smestocksensebackend.dto.forecast;

import java.time.LocalDate;

public record SalesHistorySummaryResponse(
        String source,
        long rowCount,
        long distinctProducts,
        long distinctWarehouses,
        long distinctCombinations,
        LocalDate minDate,
        LocalDate maxDate) {
}
