package com.smartflow.smestocksensebackend.dto.replenishment;

import java.math.BigDecimal;

public record CapacityGuardResult(
        Integer rawSuggestedQty,
        Integer suggestedQty,
        Boolean capacityLimited,
        Integer capacityShortfallQty,
        Integer maxAdditionalUnitsByCapacity,
        BigDecimal warehouseCapacityM3,
        BigDecimal warehouseOccupiedM3,
        BigDecimal warehouseAvailableM3,
        String configurationWarning
) {
}
