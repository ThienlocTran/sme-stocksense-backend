package com.smartflow.smestocksensebackend.dto.product;

import org.springframework.data.domain.Page;

import java.util.List;

public record ProductPageResponse(
        List<ProductListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ProductPageResponse from(Page<ProductListItemResponse> productPage) {
        return new ProductPageResponse(
                productPage.getContent(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }
}
