package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveImportReceiptDraftRequest(
        @NotNull(message = "warehouseId khong duoc de trong.")
        Long warehouseId,

        @NotNull(message = "supplierId khong duoc de trong.")
        Long supplierId,

        @Size(max = 255, message = "note khong duoc vuot qua 255 ky tu.")
        String note,

        @Valid
        List<SaveImportReceiptDraftItemRequest> items
) {
}
