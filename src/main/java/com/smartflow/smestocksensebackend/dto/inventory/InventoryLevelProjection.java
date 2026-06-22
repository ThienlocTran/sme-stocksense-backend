package com.smartflow.smestocksensebackend.dto.inventory;

import java.time.LocalDateTime;

public interface InventoryLevelProjection {
    Long getInventoryId();

    Long getProductId();

    String getProductCode();

    String getProductName();

    String getBarcode();

    Long getWarehouseId();

    String getWarehouseCode();

    String getWarehouse();

    Integer getCurrentQuantity();

    Integer getMinStock();

    Integer getMaxStock();

    String getProductStatus();

    String getWarehouseStatus();

    String getStatus();

    LocalDateTime getLastUpdatedAt();
}

        String getProductStatus();
        String getWarehouseStatus();
