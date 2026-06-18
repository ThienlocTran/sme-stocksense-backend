package com.smartflow.smestocksensebackend.dto.category;

import com.smartflow.smestocksensebackend.entity.Category;

import java.time.LocalDateTime;

public record CategoryListItemResponse(
        Long id,
        String code,
        String name,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CategoryListItemResponse from(Category category) {
        return new CategoryListItemResponse(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getDescription(),
                category.getStatus().name(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
