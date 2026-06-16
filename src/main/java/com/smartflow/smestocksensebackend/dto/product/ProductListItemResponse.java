package com.smartflow.smestocksensebackend.dto.product;

import com.smartflow.smestocksensebackend.entity.Product;

import java.math.BigDecimal;

public record ProductListItemResponse(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        String categoryName,
        String partnerName,
        String status
) {

    public static ProductListItemResponse from(Product product) {
        return new ProductListItemResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getPartner() != null ? product.getPartner().getName() : null,
                product.getStatus().name()
        );
    }
}
