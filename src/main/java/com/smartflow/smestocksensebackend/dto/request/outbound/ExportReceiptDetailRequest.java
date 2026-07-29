package com.smartflow.smestocksensebackend.dto.request.outbound;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO chứa thông tin chi tiết từng sản phẩm trong phiếu xuất nháp.
 */
@Getter
@Setter
public class ExportReceiptDetailRequest {

    @NotNull(message = "Sản phẩm không được để trống")
    private Long productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng xuất phải lớn hơn 0")
    private Integer quantity;

    @Min(value = 0, message = "Đơn giá không được âm")
    private BigDecimal unitPrice;

    private String note;
}
