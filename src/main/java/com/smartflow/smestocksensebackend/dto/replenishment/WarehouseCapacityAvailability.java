package com.smartflow.smestocksensebackend.dto.replenishment;

import java.math.BigDecimal;

public record WarehouseCapacityAvailability(
        BigDecimal warehouseCapacityM3,
        BigDecimal warehouseOccupiedM3,
        BigDecimal warehouseAvailableM3,
        Integer maxAdditionalUnitsByCapacity,
        String configurationWarning
) {
}
