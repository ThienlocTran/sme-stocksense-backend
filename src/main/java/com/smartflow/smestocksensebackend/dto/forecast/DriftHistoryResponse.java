package com.smartflow.smestocksensebackend.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriftHistoryResponse(
        Long id,
        LocalDateTime detectedAt,
        LocalDateTime checkedAt,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Integer modelVersion,
        BigDecimal actualSmape,
        BigDecimal rollingSmape,
        BigDecimal thresholdSmape,
        Boolean retrainNeeded,
        Boolean targetRetrainNeeded,
        Integer comparedDays) {
}
