package com.smartflow.smestocksensebackend.dto.dashboard;

public interface WarehouseDistributionProjection {
    Long getWarehouseId();

    String getWarehouseName();

    Long getTotalQuantity();
}
