package com.smartflow.smestocksensebackend.dto.forecast;

import java.time.LocalDate;
import java.util.List;

public record ForecastAvailabilityResponse(
        String source,
        List<Combination> combinations) {

    public record Combination(
            Long productId,
            String productCode,
            String productName,
            Long warehouseId,
            String warehouseCode,
            String warehouseName,
            Long historyDays,
            LocalDate historyStart,
            LocalDate historyEnd) {
    }
}
