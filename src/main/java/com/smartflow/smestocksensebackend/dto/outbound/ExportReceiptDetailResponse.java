package com.smartflow.smestocksensebackend.dto.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public record ExportReceiptDetailResponse(
        Long id,
        String code,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        String warehouse,
        String note,
        String status,
        String approvalLevel,
        String rejectedBy,
        LocalDateTime rejectedAt,
        String rejectionReason,
        List<ExportReceiptDetailItemResponse> items,
        Long warehouseId,
        String warehouseName,
        Long partnerId,
        String partnerName,
        Long createdById,
        String createdByName,
        String submittedByName,
        BigDecimal totalAmount,
        Long version) {

    public ExportReceiptDetailResponse(Long id, String code, String createdBy, LocalDateTime createdAt,
            LocalDateTime submittedAt, String warehouse, String note, String status, String approvalLevel,
            String rejectedBy, LocalDateTime rejectedAt, String rejectionReason,
            List<ExportReceiptDetailItemResponse> items) {
        this(id, code, createdBy, createdAt, submittedAt, warehouse, note, status, approvalLevel,
                rejectedBy, rejectedAt, rejectionReason, items, null, warehouse, null, null, null,
                createdBy, null, null, null);
    }
    public static ExportReceiptDetailResponse from(ExportReceipt receipt, List<ExportReceiptDetailItemResponse> items) {
        Employee createdBy = receipt.getCreatedBy();
        Employee rejectedBy = receipt.getRejectedBy();
        Warehouse warehouse = receipt.getWarehouse();

        return new ExportReceiptDetailResponse(
                receipt.getId(),
                receipt.getCode(),
                createdBy != null ? createdBy.getFullName() : null,
                receipt.getCreatedAt(),
                receipt.getSubmittedAt(),
                warehouse != null ? warehouse.getName() : null,
                receipt.getNote(),
                receipt.getStatus() != null ? receipt.getStatus().name() : null,
                approvalLevel(receipt),
                rejectedBy != null ? rejectedBy.getFullName() : null,
                receipt.getRejectedAt(),
                receipt.getRejectionReason(),
                items,
                warehouse != null ? warehouse.getId() : null,
                warehouse != null ? warehouse.getName() : null,
                receipt.getPartner() != null ? receipt.getPartner().getId() : null,
                receipt.getPartner() != null ? receipt.getPartner().getName() : null,
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFullName() : null,
                receipt.getSubmittedBy() != null ? receipt.getSubmittedBy().getFullName() : null,
                receipt.getTotalAmount(),
                receipt.getVersion());
    }

    private static String approvalLevel(ExportReceipt receipt) {
        ExportReceiptStatus status = receipt.getStatus();
        return status != null ? status.approvalLevel() : null;
    }
}
