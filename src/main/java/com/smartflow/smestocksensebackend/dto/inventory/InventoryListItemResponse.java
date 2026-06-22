package com.smartflow.smestocksensebackend.dto.inventory;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;

/**
 * Một dòng tồn kho trả về cho client.
 * Việc map đòi hỏi product/warehouse đã được fetch (xem InventorySpecification)
 * để tránh N+1 và LazyInitializationException.
 */
public record InventoryListItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        String unit,
        Long warehouseId,
        String warehouseName,
        Integer quantity
) {

    public static InventoryListItemResponse from(InventoryLevel level) {
        Product product = level.getProduct();
        Warehouse warehouse = level.getWarehouse();
        return new InventoryListItemResponse(
                level.getId(),
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getUnit(),
                warehouse.getId(),
                warehouse.getName(),
                level.getQuantity()
        );
    }
}
