package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImportReceiptItemResponse(
        Long id,
        Long receiptId,
        Long productId,
        String productCode,
        String productName,
        Integer quantity,
        Integer actualReceivedQuantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String note,
        String physicalStatus,
        LocalDateTime expiryDate,
        String rowStatus
) {
    // Constructor phụ tương thích ngược với các code test cũ
    public ImportReceiptItemResponse(
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
        this(
                id,
                receiptId,
                productId,
                productCode,
                productName,
                quantity,
                null,
                unitPrice,
                lineTotal,
                note,
                null,
                null,
                null
        );
    }

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
                detail.getActualReceivedQuantity(),
                detail.getExpectedUnitPrice(),
                detail.getExpectedLineTotal(),
                detail.getNote(),
                detail.getPhysicalStatus(),
                detail.getExpiryDate(),
                detail.getRowStatus()
        );
    }
}
