package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ImportReceiptDraftResponse(
        Long id,
        String code,
        Long warehouseId,
        String warehouseName,
        Long supplierId,
        String supplierName,
        Long createdById,
        String createdByName,
        Long submittedById,
        String submittedByName,
        LocalDateTime submittedAt,
        String status,
        BigDecimal totalAmount,
        String note,
        String rejectionReason,
        List<ImportReceiptItemResponse> details,
        Integer detailCount,
        LocalDateTime updatedAt,
        Long version
) {
    public ImportReceiptDraftResponse(
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
            List<ImportReceiptItemResponse> details,
            Integer detailCount,
            LocalDateTime updatedAt,
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
                null,
                null,
                null,
                status,
                totalAmount,
                note,
                null,
                details,
                detailCount,
                updatedAt,
                version
        );
    }

    public ImportReceiptDraftResponse(
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
            String rejectionReason,
            List<ImportReceiptItemResponse> details,
            Integer detailCount,
            LocalDateTime updatedAt,
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
                null,
                null,
                null,
                status,
                totalAmount,
                note,
                rejectionReason,
                details,
                detailCount,
                updatedAt,
                version
        );
    }

    public ImportReceiptDraftResponse(
            Long id,
            String code,
            Long warehouseId,
            String warehouseName,
            Long supplierId,
            String supplierName,
            Long createdById,
            String createdByName,
            Long submittedById,
            String submittedByName,
            LocalDateTime submittedAt,
            String status,
            BigDecimal totalAmount,
            String note,
            List<ImportReceiptItemResponse> details,
            Integer detailCount,
            LocalDateTime updatedAt,
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
                submittedById,
                submittedByName,
                submittedAt,
                status,
                totalAmount,
                note,
                null,
                details,
                detailCount,
                updatedAt,
                version
        );
    }

    public static ImportReceiptDraftResponse from(ImportReceipt receipt, List<ImportReceiptDetail> details) {
        Warehouse warehouse = receipt.getWarehouse();
        Partner supplier = receipt.getSupplier();
        Employee createdBy = receipt.getCreatedBy();
        Employee submittedBy = receipt.getSubmittedBy();
        List<ImportReceiptItemResponse> itemResponses = details.stream()
                .map(ImportReceiptItemResponse::from)
                .toList();

        return new ImportReceiptDraftResponse(
                receipt.getId(),
                receipt.getCode(),
                warehouse != null ? warehouse.getId() : null,
                warehouse != null ? warehouse.getName() : null,
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getName() : null,
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFullName() : null,
                submittedBy != null ? submittedBy.getId() : null,
                submittedBy != null ? submittedBy.getFullName() : null,
                receipt.getSubmittedAt(),
                receipt.getStatus() != null ? receipt.getStatus().name() : null,
                receipt.getTotalAmount(),
                receipt.getNote(),
                rejectionReason(receipt),
                itemResponses,
                itemResponses.size(),
                receipt.getUpdatedAt(),
                receipt.getVersion()
        );
    }

    private static String rejectionReason(ImportReceipt receipt) {
        return receipt.getStatus() == ImportReceiptStatus.TU_CHOI ? receipt.getRejectionReason() : null;
    }
}
