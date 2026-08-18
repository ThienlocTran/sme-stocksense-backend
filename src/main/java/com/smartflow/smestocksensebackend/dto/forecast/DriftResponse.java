package com.smartflow.smestocksensebackend.dto.forecast;

import java.math.BigDecimal;

public record DriftResponse(
        Long productId,
        Long warehouseId,
        String status,
        BigDecimal rollingSmape,
        BigDecimal threshold,
        boolean retrainNeeded,
        int overlapDays) {
}
