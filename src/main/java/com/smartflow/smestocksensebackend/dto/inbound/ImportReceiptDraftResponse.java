package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
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
        String status,
        BigDecimal totalAmount,
        String note,
        List<ImportReceiptItemResponse> details,
        Integer detailCount,
        LocalDateTime updatedAt,
        Long version
) {
    public static ImportReceiptDraftResponse from(ImportReceipt receipt, List<ImportReceiptDetail> details) {
        Warehouse warehouse = receipt.getWarehouse();
        Partner supplier = receipt.getSupplier();
        Employee createdBy = receipt.getCreatedBy();
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
                receipt.getStatus() != null ? receipt.getStatus().name() : null,
                receipt.getTotalAmount(),
                receipt.getNote(),
                itemResponses,
                itemResponses.size(),
                receipt.getUpdatedAt(),
                receipt.getVersion()
        );
    }
}
