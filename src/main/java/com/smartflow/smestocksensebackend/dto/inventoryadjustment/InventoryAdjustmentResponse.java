package com.smartflow.smestocksensebackend.dto.inventoryadjustment;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryCount;
import com.smartflow.smestocksensebackend.entity.InventoryCountDetail;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryAdjustmentResponse(
        Long id,
        String code,
        String status,
        CountInfo inventoryCount,
        String countCode,
        Long warehouseId,
        String warehouseName,
        Long createdById,
        String createdByName,
        Long submittedById,
        String submittedByName,
        Long approvedById,
        String approvedByName,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime appliedAt,
        String note,
        String rejectionReason,
        Long version,
        List<Detail> details
) {
    public record CountInfo(Long id, String code, Long warehouseId, String warehouseName) {}

    public record Detail(
            Long id,
            Long productId,
            String productCode,
            String productName,
            Integer systemQuantity,
            Integer actualQuantity,
            Integer differenceQuantity,
            Integer adjustmentQuantity,
            String reason,
            String discrepancyReason,
            String note
    ) {
        static Detail from(InventoryCountDetail detail) {
            return new Detail(
                    detail.getId(),
                    detail.getProduct().getId(),
                    detail.getProduct().getCode(),
                    detail.getProduct().getName(),
                    detail.getSystemQuantity(),
                    detail.getActualQuantity(),
                    detail.getDifferenceQuantity(),
                    detail.getDifferenceQuantity(),
                    detail.getReason(),
                    detail.getReason(),
                    detail.getNote()
            );
        }
    }

    public static InventoryAdjustmentResponse from(InventoryAdjustment adjustment, List<InventoryCountDetail> details) {
        InventoryCount count = adjustment.getInventoryCount();
        Employee createdBy = adjustment.getCreatedBy();
        Employee submittedBy = adjustment.getSubmittedBy();
        Employee approvedBy = adjustment.getApprovedBy();

        return new InventoryAdjustmentResponse(
                adjustment.getId(),
                adjustment.getCode(),
                adjustment.getStatus().name(),
                new CountInfo(count.getId(), count.getCode(), count.getWarehouse().getId(), count.getWarehouse().getName()),
                count.getCode(),
                count.getWarehouse().getId(),
                count.getWarehouse().getName(),
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFullName() : null,
                submittedBy != null ? submittedBy.getId() : null,
                submittedBy != null ? submittedBy.getFullName() : null,
                approvedBy != null ? approvedBy.getId() : null,
                approvedBy != null ? approvedBy.getFullName() : null,
                adjustment.getCreatedAt(),
                adjustment.getSubmittedAt(),
                adjustment.getApprovedAt(),
                adjustment.getAppliedAt(),
                adjustment.getNote(),
                adjustment.getRejectionReason(),
                adjustment.getVersion(),
                details.stream()
                        .filter(detail -> detail.getDifferenceQuantity() != null && detail.getDifferenceQuantity() != 0)
                        .map(Detail::from)
                        .toList()
        );
    }
}
