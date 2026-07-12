package com.smartflow.smestocksensebackend.dto.replenishment;

public record ReplenishmentSuggestionResponse(
        Long productId, String productCode, String productName,
        Long warehouseId, String warehouseCode, String warehouseName,
        Integer currentStock, Integer minStock, Integer maxStock,
        Integer shortageQuantity, Integer suggestedQuantity,
        ReplenishmentReason reason, ReplenishmentPriority priority,
        String configurationWarning) {
}
