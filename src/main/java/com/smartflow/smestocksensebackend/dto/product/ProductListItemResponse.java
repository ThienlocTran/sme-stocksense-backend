package com.smartflow.smestocksensebackend.dto.product;

import com.smartflow.smestocksensebackend.entity.Product;

import java.time.LocalDateTime;

public record ProductListItemResponse(
        Long id,
        String code,
        String sku,
        String name,
        Long categoryId,
        String categoryName,
        String unit,
        Integer minThreshold,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductListItemResponse from(Product p) {
        return new ProductListItemResponse(
                p.getId(),
                p.getCode(),
                p.getSku(),
                p.getName(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getUnit(),
                p.getMinThreshold(),
                p.getActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
