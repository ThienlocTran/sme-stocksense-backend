package com.smartflow.smestocksensebackend.dto.response.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceiptItem;

import java.math.BigDecimal;

public record ExportReceiptItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        String unit,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String note,
        Integer availableStock,
        boolean exceedsAvailableStock) {

    public static ExportReceiptItemResponse from(ExportReceiptItem item, int availableStock) {
        return new ExportReceiptItemResponse(
                item.getId(), item.getProduct().getId(), item.getProduct().getCode(),
                item.getProduct().getName(), item.getProduct().getUnit(), item.getQuantity(),
                item.getUnitPrice(), item.getLineTotal(), item.getNote(), availableStock,
                item.getQuantity() != null && item.getQuantity() > availableStock);
    }
}
