package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

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
