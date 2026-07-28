package com.smartflow.smestocksensebackend.dto.mapper;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;

/**
 * Mapper chuyển đổi InventoryAlert Entity sang DTO.
 * Tách biệt mapping logic để tránh DTO phụ thuộc vào Entity.
 */
public final class InventoryAlertMapper {

    private InventoryAlertMapper() {
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO.
     */
    public static InventoryAlertResponse toResponse(InventoryAlert entity) {
        if (entity == null) {
            return null;
        }

        var product = entity.getProduct();
        var warehouse = entity.getWarehouse();

        return InventoryAlertResponse.builder()
                .id(entity.getId())
                .productId(product != null ? product.getId() : null)
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .warehouseId(warehouse != null ? warehouse.getId() : null)
                .warehouseCode(warehouse != null ? warehouse.getCode() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .currentQuantity(entity.getCurrentQuantity())
                .minStock(entity.getMinStock())
                .severity(entity.getSeverity())
                .status(entity.getStatus())
                .note(entity.getNote())
                .handledBy(entity.getHandledBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
