package com.smartflow.smestocksensebackend.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ForecastResponse(
        Long productId,
        Long warehouseId,
        Integer version,
        String mode,
        BigDecimal smape,
        BigDecimal forecast7d,
        BigDecimal forecast14d,
        BigDecimal forecast30d,
        Integer currentStock,
        Integer minStock,
        Integer reorderQty7d,
        Integer reorderQty14d,
        Integer reorderQty30d,
        Integer dataDays,
        LocalDateTime trainedAt) {
}
