package com.smartflow.smestocksensebackend.dto.inventory;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    BigDecimal getUnitVolumeM3();

    String getProductStatus();

    String getWarehouseStatus();

    String getStatus();

    LocalDateTime getLastUpdatedAt();
}
