package com.smartflow.smestocksensebackend.dto.inventory;

/**
 * Tiêu chí lọc tồn kho (T66/T67). Bind từ query params qua @ModelAttribute.
 *
 * @param warehouseId lọc theo id kho (nullable)
 * @param categoryId  lọc theo id danh mục của sản phẩm (nullable)
 * @param keyword     tìm gần đúng theo mã / tên sản phẩm (nullable)
 * @param minQuantity số lượng tồn tối thiểu (nullable)
 * @param maxQuantity số lượng tồn tối đa (nullable)
 */
public record InventoryFilterRequest(
        Long warehouseId,
        Long categoryId,
        String keyword,
        Integer minQuantity,
        Integer maxQuantity
) {
}
