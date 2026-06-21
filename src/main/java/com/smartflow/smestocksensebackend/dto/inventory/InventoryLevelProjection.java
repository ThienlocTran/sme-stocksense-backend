package com.smartflow.smestocksensebackend.dto.inventory;

import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import java.time.LocalDateTime;

public interface InventoryLevelProjection {
    Long getInventoryId();
    Long getProductId();
    String getProductCode();
    String getProductName();
    String getBarcode();
    Long getWarehouseId();
    String getWarehouseCode();
    String getWarehouseName();
    Integer getQuantity();
    Integer getMinStock();
    Integer getMaxStock();
    ProductStatus getProductStatus();
    WarehouseStatus getWarehouseStatus();
    String getStockStatus();
    LocalDateTime getLastUpdatedAt();
}
