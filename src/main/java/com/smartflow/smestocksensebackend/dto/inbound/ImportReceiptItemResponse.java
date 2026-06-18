package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.Product;

import java.math.BigDecimal;

public record ImportReceiptItemResponse(
        Long id,
        Long receiptId,
        Long productId,
        String productCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String note
) {
    public static ImportReceiptItemResponse from(ImportReceiptDetail detail) {
        ImportReceipt receipt = detail.getDocument();
        Product product = detail.getProduct();

        return new ImportReceiptItemResponse(
                detail.getId(),
                receipt != null ? receipt.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getCode() : null,
                product != null ? product.getName() : null,
                detail.getExpectedQuantity(),
                detail.getExpectedUnitPrice(),
                detail.getExpectedLineTotal(),
                detail.getNote()
        );
    }
}
