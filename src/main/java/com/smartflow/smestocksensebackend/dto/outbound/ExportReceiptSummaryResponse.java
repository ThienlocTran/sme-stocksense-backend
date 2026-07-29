package com.smartflow.smestocksensebackend.dto.outbound;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record ExportReceiptSummaryResponse(
        Long id,
        String code,
        Long warehouseId,
        String warehouseName,
        Long createdById,
        String createdByName,
        String status,
        String approvalLevel,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        Long partnerId,
        String partnerName,
        BigDecimal totalAmount,
        Long version,
        String rejectionReason) {

    public ExportReceiptSummaryResponse(Long id, String code, Long warehouseId, String warehouseName,
            Long createdById, String createdByName, String status, String approvalLevel,
            LocalDateTime submittedAt, LocalDateTime createdAt) {
        this(id, code, warehouseId, warehouseName, createdById, createdByName, status, approvalLevel,
                submittedAt, createdAt, null, null, null, null, null);
    }
    public static ExportReceiptSummaryResponse from(ExportReceipt receipt) {
        Warehouse warehouse = receipt.getWarehouse();
        Employee createdBy = receipt.getCreatedBy();

        return new ExportReceiptSummaryResponse(
                receipt.getId(),
                receipt.getCode(),
                warehouse != null ? warehouse.getId() : null,
                warehouse != null ? warehouse.getName() : null,
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFullName() : null,
                receipt.getStatus() != null ? receipt.getStatus().name() : null,
                approvalLevel(receipt),
                receipt.getSubmittedAt(),
                receipt.getCreatedAt(),
                receipt.getPartner() != null ? receipt.getPartner().getId() : null,
                receipt.getPartner() != null ? receipt.getPartner().getName() : null,
                receipt.getTotalAmount(),
                receipt.getVersion(),
                receipt.getRejectionReason());
    }

    private static String approvalLevel(ExportReceipt receipt) {
        ExportReceiptStatus status = receipt.getStatus();
        return status != null ? status.approvalLevel() : null;
    }
}
