package com.smartflow.smestocksensebackend.dto.response.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ponytail: Dùng record thay vì class cồng kềnh, tránh sinh boilerplate Get/Set.
public record ExportReceiptSummaryResponse(
    Long id,
    String code,
    String warehouseName,
    String status,
    BigDecimal totalAmount,
    String createdBy,
    LocalDateTime createdAt,
    Long version,
    String partnerName,
    String rejectionReason
) {
    public ExportReceiptSummaryResponse(Long id, String code, String warehouseName, String status,
            BigDecimal totalAmount, String createdBy, LocalDateTime createdAt) {
        this(id, code, warehouseName, status, totalAmount, createdBy, createdAt, null, null, null);
    }
    public static ExportReceiptSummaryResponse from(ExportReceipt r) {
        return new ExportReceiptSummaryResponse(
            r.getId(),
            r.getCode(),
            r.getWarehouse() != null ? r.getWarehouse().getName() : null,
            r.getStatus().name(),
            r.getTotalAmount(),
            r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null,
            r.getCreatedAt(),
            r.getVersion(),
            r.getPartner() != null ? r.getPartner().getName() : null,
            r.getRejectionReason()
        );
    }
}
