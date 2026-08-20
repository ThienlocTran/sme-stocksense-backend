package com.smartflow.smestocksensebackend.dto.inventory;

import java.time.LocalDateTime;

public class InventoryLevelResponse {

        private final Long inventoryId;
        private final Long productId;
        private final String productCode;
        private final String productName;
        private final String barcode;
        private final Long warehouseId;
        private final String warehouseCode;
        private final String warehouse;
        private final Integer currentQuantity;
        private final Integer minStock;
        private final Integer maxStock;
        private final String productStatus;
        private final String warehouseStatus;
        private final String status;
        private final LocalDateTime lastUpdatedAt;
        private final java.math.BigDecimal unitVolumeM3;

        public InventoryLevelResponse(
                        Long inventoryId,
                        Long productId,
                        String productCode,
                        String productName,
                        String barcode,
                        Long warehouseId,
                        String warehouseCode,
                        String warehouse,
                        Integer currentQuantity,
                        Integer minStock,
                        Integer maxStock,
                        String productStatus,
                        String warehouseStatus,
                        String status,
                        LocalDateTime lastUpdatedAt) {
                this(inventoryId, productId, productCode, productName, barcode, warehouseId, warehouseCode, warehouse,
                     currentQuantity, minStock, maxStock, productStatus, warehouseStatus, status, lastUpdatedAt, null);
        }

        public InventoryLevelResponse(
                        Long inventoryId,
                        Long productId,
                        String productCode,
                        String productName,
                        String barcode,
                        Long warehouseId,
                        String warehouseCode,
                        String warehouse,
                        Integer currentQuantity,
                        Integer minStock,
                        Integer maxStock,
                        String productStatus,
                        String warehouseStatus,
                        String status,
                        LocalDateTime lastUpdatedAt,
                        java.math.BigDecimal unitVolumeM3) {
                this.inventoryId = inventoryId;
                this.productId = productId;
                this.productCode = productCode;
                this.productName = productName;
                this.barcode = barcode;
                this.warehouseId = warehouseId;
                this.warehouseCode = warehouseCode;
                this.warehouse = warehouse;
                this.currentQuantity = currentQuantity;
                this.minStock = minStock;
                this.maxStock = maxStock;
                this.productStatus = productStatus;
                this.warehouseStatus = warehouseStatus;
                this.status = status;
                this.lastUpdatedAt = lastUpdatedAt;
                this.unitVolumeM3 = unitVolumeM3;
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

        public String getWarehouse() {
                return warehouse;
        }

        public Integer getCurrentQuantity() {
                return currentQuantity;
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

        public String getStatus() {
                return status;
        }

        public LocalDateTime getLastUpdatedAt() {
                return lastUpdatedAt;
        }

        public java.math.BigDecimal getUnitVolumeM3() {
                return unitVolumeM3;
        }
}
