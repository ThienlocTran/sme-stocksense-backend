package com.smartflow.smestocksensebackend.dto.inventory;

import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import java.time.LocalDateTime;

public class InventoryLevelResponse {

        private final Long inventoryId;
        private final Long productId;
        private final String productCode;
        private final String productName;
        private final String barcode;
        private final Long warehouseId;
        private final String warehouseCode;
        private final String warehouseName;
        private final Integer quantity;
        private final Integer minStock;
        private final Integer maxStock;
        private final ProductStatus productStatus;
        private final WarehouseStatus warehouseStatus;
        private final String stockStatus;
        private final LocalDateTime lastUpdatedAt;

        public InventoryLevelResponse(
                        Long inventoryId,
                        Long productId,
                        String productCode,
                        String productName,
                        String barcode,
                        Long warehouseId,
                        String warehouseCode,
                        String warehouseName,
                        Integer quantity,
                        Integer minStock,
                        Integer maxStock,
                        ProductStatus productStatus,
                        WarehouseStatus warehouseStatus,
                        String stockStatus,
                        LocalDateTime lastUpdatedAt) {
                this.inventoryId = inventoryId;
                this.productId = productId;
                this.productCode = productCode;
                this.productName = productName;
                this.barcode = barcode;
                this.warehouseId = warehouseId;
                this.warehouseCode = warehouseCode;
                this.warehouseName = warehouseName;
                this.quantity = quantity;
                this.minStock = minStock;
                this.maxStock = maxStock;
                this.productStatus = productStatus;
                this.warehouseStatus = warehouseStatus;
                this.stockStatus = stockStatus;
                this.lastUpdatedAt = lastUpdatedAt;
        }

        public Long getInventoryId() {
                return inventoryId;
        }

        public Long getProductId() {
                return productId;
        }

        public String getProductCode() {
                return productCode;
        }

        public String getProductName() {
                return productName;
        }

        public String getBarcode() {
                return barcode;
        }

        public Long getWarehouseId() {
                return warehouseId;
        }

        public String getWarehouseCode() {
                return warehouseCode;
        }

        public String getWarehouseName() {
                return warehouseName;
        }

        public Integer getQuantity() {
                return quantity;
        }

        public Integer getMinStock() {
                return minStock;
        }

        public Integer getMaxStock() {
                return maxStock;
        }

        public String getProductStatus() {
                return productStatus;
        }

        public String getWarehouseStatus() {
                return warehouseStatus;
        }

        public String getStockStatus() {
                return stockStatus;
        }

        public LocalDateTime getLastUpdatedAt() {
                return lastUpdatedAt;
        }
}
