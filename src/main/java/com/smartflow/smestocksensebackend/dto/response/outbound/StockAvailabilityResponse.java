package com.smartflow.smestocksensebackend.dto.response.outbound;

public record StockAvailabilityResponse(
        Long warehouseId,
        Long productId,
        Integer availableStock) {
}
