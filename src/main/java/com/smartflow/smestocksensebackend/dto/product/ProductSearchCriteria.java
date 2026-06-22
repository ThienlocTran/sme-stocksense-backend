package com.smartflow.smestocksensebackend.dto.product;

import com.smartflow.smestocksensebackend.entity.ProductStatus;

/**
 * Tiêu chí tìm kiếm / lọc sản phẩm.
 * Tách riêng khỏi Service để Controller chỉ việc gom params và truyền xuống Specification.
 *
 * @param keyword    tìm gần đúng theo code / name / sku (nullable)
 * @param categoryId lọc theo id danh mục (nullable)
 * @param status     lọc theo trạng thái đã được parse sang enum (nullable)
 */
public record ProductSearchCriteria(
        String keyword,
        Long categoryId,
        ProductStatus status
) {
}
