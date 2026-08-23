package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateImportReceiptRequest(
        @NotNull(message = "warehouseId không được để trống.")
        Long warehouseId,

        @NotNull(message = "supplierId không được để trống.")
        Long supplierId,

        @Size(max = 255, message = "note không được vượt quá 255 ký tự.")
        String note,

        Long aiPurchaseRequestId
) {
    public CreateImportReceiptRequest(Long warehouseId, Long supplierId, String note) {
        this(warehouseId, supplierId, note, null);
    }
}
