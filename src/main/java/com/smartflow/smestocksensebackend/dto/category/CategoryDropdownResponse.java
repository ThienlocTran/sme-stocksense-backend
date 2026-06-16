package com.smartflow.smestocksensebackend.dto.category;

import com.smartflow.smestocksensebackend.entity.Category;

public record CategoryDropdownResponse(
        Long id,
        String tenDanhMuc,
        String trangThai
) {

    public static CategoryDropdownResponse from(Category category) {
        return new CategoryDropdownResponse(
                category.getId(),
                category.getName(),
                category.getStatus().name()
        );
    }
}
