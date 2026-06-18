package com.smartflow.smestocksensebackend.dto.category;

import org.springframework.data.domain.Page;

import java.util.List;

public record CategoryPageResponse(
        List<CategoryListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static CategoryPageResponse from(Page<CategoryListItemResponse> categoryPage) {
        return new CategoryPageResponse(
                categoryPage.getContent(),
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages()
        );
    }
}
