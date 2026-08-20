package com.smartflow.smestocksensebackend.dto.product;

import com.smartflow.smestocksensebackend.entity.Product;

import java.math.BigDecimal;

/**
 * Chi tiết sản phẩm phục vụ form sửa: trả về categoryId/partnerId (id) thay vì tên,
 * để frontend bind đúng v-model của dropdown.
 */
public record ProductDetailResponse(
        Long id,
        String code,
        String name,
        String sku,
        String barcode,
        String unit,
        BigDecimal price,
        BigDecimal unitVolumeM3,
        Integer defaultMinStock,
        Long categoryId,
        String categoryName,
        Long partnerId,
        String partnerName,
        String status
) {

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getSku(),
                product.getBarcode(),
                product.getUnit(),
                product.getPrice(),
                product.getUnitVolumeM3(),
                product.getDefaultMinStock(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getPartner() != null ? product.getPartner().getId() : null,
                product.getPartner() != null ? product.getPartner().getName() : null,
                product.getStatus().name()
        );
    }
}
