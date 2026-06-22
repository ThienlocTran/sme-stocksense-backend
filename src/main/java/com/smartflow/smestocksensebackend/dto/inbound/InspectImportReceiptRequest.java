package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InspectImportReceiptRequest(
        @NotEmpty(message = "Danh sách sản phẩm kiểm hàng không được để trống.")
        @Valid
        List<InspectImportReceiptItemRequest> items
) {
}
