package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Yêu cầu kiểm hàng thực tế cho phiếu nhập kho (Import Receipt Inspection Request).
 * Chứa danh sách các thông tin kiểm đếm thực nhận cho từng dòng sản phẩm.
 *
 * @param items Danh sách chi tiết thông tin kiểm hàng của từng sản phẩm
 */
public record InspectImportReceiptRequest(
        @NotEmpty(message = "Danh sách sản phẩm kiểm hàng không được để trống.")
        @Valid
        List<@NotNull @Valid InspectImportReceiptItemRequest> items
) {
}
