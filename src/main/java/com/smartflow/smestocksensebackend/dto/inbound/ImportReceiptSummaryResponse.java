package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImportReceiptSummaryResponse(
        Long id,
        String code,
        Long warehouseId,
        String warehouseName,
        Long supplierId,
        String supplierName,
        Long createdById,
        String createdByName,
        String status,
        BigDecimal totalAmount,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime submittedAt,
        LocalDateTime cancelledAt,
        LocalDateTime actualArrivalDate,
        Long version
) {
    public ImportReceiptSummaryResponse(
            Long id,
            String code,
            Long warehouseId,
            String warehouseName,
            Long supplierId,
            String supplierName,
            Long createdById,
            String createdByName,
            String status,
            BigDecimal totalAmount,
            String note,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime submittedAt,
            LocalDateTime cancelledAt,
            Long version
    ) {
        this(
                id,
                code,
                warehouseId,
                warehouseName,
                supplierId,
                supplierName,
                createdById,
                createdByName,
                status,
                totalAmount,
                note,
                createdAt,
                updatedAt,
                submittedAt,
                cancelledAt,
                null,
                version
        );
    }

    public static ImportReceiptSummaryResponse from(ImportReceipt receipt) {
        Warehouse warehouse = receipt.getWarehouse();
        Partner supplier = receipt.getSupplier();
        Employee createdBy = receipt.getCreatedBy();

        return new ImportReceiptSummaryResponse(
                receipt.getId(),
                receipt.getCode(),
                warehouse != null ? warehouse.getId() : null,
                warehouse != null ? warehouse.getName() : null,
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getName() : null,
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFullName() : null,
                receipt.getStatus() != null ? receipt.getStatus().name() : null,
                receipt.getTotalAmount(),
                receipt.getNote(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt(),
                receipt.getSubmittedAt(),
                receipt.getCancelledAt(),
                receipt.getActualArrivalDate(),
                receipt.getVersion()
        );
    }
}
