package com.smartflow.smestocksensebackend.dto.replenishment;

import java.math.BigDecimal;

public record ForecastReplenishmentRecommendationResponse(
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Short horizonDays,
        BigDecimal forecastDemand,
        Integer currentStock,
        Integer effectiveMinStock,
        Integer rawSuggestedQty,
        Integer suggestedQty,
        Boolean capacityLimited,
        Integer capacityShortfallQty,
        Integer maxAdditionalUnitsByCapacity,
        BigDecimal warehouseCapacityM3,
        BigDecimal warehouseOccupiedM3,
        BigDecimal warehouseAvailableM3,
        Long modelMetadataId,
        Integer modelVersion,
        String capacityWarning
) {
}
