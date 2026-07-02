package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Yêu cầu chi tiết kiểm hàng cho từng sản phẩm trong phiếu nhập kho.
 * Chứa số lượng thực tế nhận được, tình trạng vật lý và hạn sử dụng của sản phẩm.
 *
 * @param productId ID của sản phẩm cần kiểm đếm
 * @param actualReceivedQuantity Số lượng sản phẩm thực tế nhận được (phải >= 0)
 * @param physicalStatus Mô tả tình trạng vật lý (ví dụ: nguyên vẹn, móp méo, vỡ...)
 * @param expiryDate Hạn sử dụng của lô hàng thực nhận (nếu có)
 */
public record InspectImportReceiptItemRequest(
        @NotNull(message = "productId không được để trống.")
        Long productId,

        @NotNull(message = "actualReceivedQuantity không được để trống.")
        @Min(value = 0, message = "actualReceivedQuantity phải lớn hơn hoặc bằng 0.")
        Integer actualReceivedQuantity,

        @Size(max = 255, message = "physicalStatus không được vượt quá 255 ký tự.")
        String physicalStatus,

        LocalDateTime expiryDate
) {
}
