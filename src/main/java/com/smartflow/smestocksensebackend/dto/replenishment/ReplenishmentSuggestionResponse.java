package com.smartflow.smestocksensebackend.dto.replenishment;

public record ReplenishmentSuggestionResponse(
        Long productId, String productCode, String productName,
        Long warehouseId, String warehouseCode, String warehouseName,
        Integer currentStock, Integer minStock, Integer maxStock,
        Integer shortageQuantity, Integer suggestedQuantity,
        ReplenishmentReason reason, ReplenishmentPriority priority,
        String configurationWarning,
        java.math.BigDecimal unitVolumeM3,
        Integer capacityAllowedQuantity,
        Boolean capacityLimited) {

    public ReplenishmentSuggestionResponse(
            Long productId, String productCode, String productName,
            Long warehouseId, String warehouseCode, String warehouseName,
            Integer currentStock, Integer minStock, Integer maxStock,
            Integer shortageQuantity, Integer suggestedQuantity,
            ReplenishmentReason reason, ReplenishmentPriority priority,
            String configurationWarning) {
        this(productId, productCode, productName, warehouseId, warehouseCode, warehouseName,
             currentStock, minStock, maxStock, shortageQuantity, suggestedQuantity,
             reason, priority, configurationWarning, null, suggestedQuantity, false);
    }
}
