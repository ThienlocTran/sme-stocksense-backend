package com.smartflow.smestocksensebackend.dto.request.outbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Payload dùng để tạo mới phiếu xuất kho (Draft).
 */
@Getter
@Setter
public class ExportReceiptDraftRequest {

    @NotNull(message = "Kho xuất không được để trống")
    private Long warehouseId;

    private Long partnerId; // Khách hàng (có thể null nếu xuất nội bộ)

    private String note;

    @NotEmpty(message = "Danh sách sản phẩm không được rỗng")
    @Valid
    private List<ExportReceiptDetailRequest> details;
}
