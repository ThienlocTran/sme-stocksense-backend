package com.smartflow.smestocksensebackend.dto.inventorycount;

import com.smartflow.smestocksensebackend.entity.*;
import java.time.LocalDateTime;
import java.util.List;

public record InventoryCountResponse(Long id, String code, Long warehouseId, String warehouseName, String status,
        String note, Long createdById, String createdByName, LocalDateTime createdAt, LocalDateTime finalizedAt,
        LocalDateTime cancelledAt, String cancellationReason, Long version, List<Detail> details) {
    public record Detail(Long id, Long productId, String productCode, String productName, Integer systemQuantity,
            Integer actualQuantity, Integer differenceQuantity, String note, Long version) {
        public static Detail from(InventoryCountDetail d) { return new Detail(d.getId(), d.getProduct().getId(),
                d.getProduct().getCode(), d.getProduct().getName(), d.getSystemQuantity(), d.getActualQuantity(),
                d.getDifferenceQuantity(), d.getNote(), d.getVersion()); }
    }
    public static InventoryCountResponse from(InventoryCount c, List<InventoryCountDetail> lines) {
        return new InventoryCountResponse(c.getId(), c.getCode(), c.getWarehouse().getId(), c.getWarehouse().getName(),
                c.getStatus().name(), c.getNote(), c.getCreatedBy().getId(), c.getCreatedBy().getFullName(), c.getCreatedAt(),
                c.getFinalizedAt(), c.getCancelledAt(), c.getCancellationReason(), c.getVersion(), lines.stream().map(Detail::from).toList());
    }
}
