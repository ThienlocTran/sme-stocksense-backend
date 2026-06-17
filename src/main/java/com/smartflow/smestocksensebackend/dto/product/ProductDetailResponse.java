package com.smartflow.smestocksensebackend.dto.product;

import com.smartflow.smestocksensebackend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO chi tiết sản phẩm cho GET /api/products/{id}.
 * Tên field khớp với frontend (ProductCreateView.loadProduct) và
 * nhất quán với ProductListItemResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private String code;
    private String name;
    private String sku;
    private String barcode;
    private String unit;
    private BigDecimal price;
    private Integer minStock;

    private Long categoryId;
    private String categoryName;

    private Long partnerId;
    private String partnerName;

    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductDetailResponse from(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .unit(product.getUnit())
                .price(product.getPrice())
                .minStock(product.getMinStock())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .partnerId(product.getPartner() != null ? product.getPartner().getId() : null)
                .partnerName(product.getPartner() != null ? product.getPartner().getName() : null)
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
