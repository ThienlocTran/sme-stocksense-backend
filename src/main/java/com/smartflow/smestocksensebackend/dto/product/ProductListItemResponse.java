package com.smartflow.smestocksensebackend.dto.product;

import com.smartflow.smestocksensebackend.entity.Product;

import java.math.BigDecimal;

public record ProductListItemResponse(
        Long id,
        String code,
        String name,
        String sku,
        String barcode,
        String unit,
        BigDecimal unitVolumeM3,
        Integer defaultMinStock,
        BigDecimal price,
        String categoryName,
        String partnerName,
        String status
) {

    public static ProductListItemResponse from(Product product) {
        return new ProductListItemResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getUnit(),
                product.getUnitVolumeM3(),
                product.getDefaultMinStock(),
                product.getPrice(),
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getPartner() != null ? product.getPartner().getName() : null,
                product.getStatus().name()
        );
    }
}
