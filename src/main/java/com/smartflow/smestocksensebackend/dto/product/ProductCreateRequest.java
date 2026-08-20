package com.smartflow.smestocksensebackend.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống.")
        String name,

        @NotBlank(message = "Mã sản phẩm không được để trống.")
        String code,

        String sku,

        String barcode,

        @NotBlank(message = "Đơn vị tính không được để trống.")
        String unit,

        @NotNull(message = "Giá tiền không được để trống.")
        @Min(value = 0, message = "Giá tiền phải lớn hơn hoặc bằng 0.")
        BigDecimal price,

        Long categoryId,

        Long partnerId,

        BigDecimal unitVolumeM3,

        @NotNull(message = "Tồn tối thiểu mặc định không được để trống.")
        @PositiveOrZero(message = "Tồn tối thiểu mặc định phải lớn hơn hoặc bằng 0.")
        Integer defaultMinStock
) {
}
