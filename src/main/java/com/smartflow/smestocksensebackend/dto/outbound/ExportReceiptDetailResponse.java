package com.smartflow.smestocksensebackend.dto.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;

import java.time.LocalDateTime;
import java.util.List;

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
        List<ExportReceiptDetailItemResponse> items) {
    public static ExportReceiptDetailResponse from(ExportReceipt receipt, List<ExportReceiptDetailItemResponse> items) {
        Employee createdBy = receipt.getCreatedBy();
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
                items);
    }

    private static String approvalLevel(ExportReceipt receipt) {
        ExportReceiptStatus status = receipt.getStatus();
        return status != null ? status.approvalLevel() : null;
    }
}
