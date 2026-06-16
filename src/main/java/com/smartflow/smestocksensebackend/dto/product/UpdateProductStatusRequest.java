package com.smartflow.smestocksensebackend.dto.product;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductStatusRequest(
        @NotBlank(message = "Trạng thái không được để trống.")
        String trangThai
) {
}
