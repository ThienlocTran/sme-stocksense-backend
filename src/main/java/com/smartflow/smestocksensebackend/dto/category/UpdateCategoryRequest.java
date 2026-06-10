package com.smartflow.smestocksensebackend.dto.category;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        @NotBlank(message = "Mã danh mục không được để trống.")
        String code,

        @NotBlank(message = "Tên danh mục không được để trống.")
        String name,

        String description,

        String status
) {
}
