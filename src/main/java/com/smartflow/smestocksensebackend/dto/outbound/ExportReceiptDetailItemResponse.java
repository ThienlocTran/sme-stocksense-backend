package com.smartflow.smestocksensebackend.dto.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.Product;
import java.math.BigDecimal;

public record ExportReceiptDetailItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        String unit,
        Integer quantity,
        Integer currentInventory,
        Boolean warning,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String note) {

    public ExportReceiptDetailItemResponse(Long id, Long productId, String productCode, String productName,
            String unit, Integer quantity, Integer currentInventory, Boolean warning) {
        this(id, productId, productCode, productName, unit, quantity, currentInventory, warning, null, null, null);
    }
    public static ExportReceiptDetailItemResponse from(ExportReceiptDetail detail, Integer currentInventory,
            boolean warning) {
        Product product = detail.getProduct();
        return new ExportReceiptDetailItemResponse(
                detail.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getCode() : null,
                product != null ? product.getName() : null,
                product != null ? product.getUnit() : null,
                detail.getQuantity(),
                currentInventory,
                warning,
                detail.getUnitPrice(),
                detail.getLineTotal(),
                detail.getNote());
    }
}
