package com.smartflow.smestocksensebackend.dto.response;

public record WarehouseDistributionResponse(
        Long warehouseId,
        String warehouseName,
        Long totalQuantity
) {
}
