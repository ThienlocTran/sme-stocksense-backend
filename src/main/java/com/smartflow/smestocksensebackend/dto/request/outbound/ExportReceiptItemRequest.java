package com.smartflow.smestocksensebackend.dto.request.outbound;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExportReceiptItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        @DecimalMin("0.0") BigDecimal unitPrice,
        String note) {
}
