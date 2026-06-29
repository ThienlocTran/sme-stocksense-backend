package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu từ chối phiếu nhập kho đang chờ duyệt (T94).
 * Lý do từ chối là bắt buộc để nhân viên lập phiếu biết được nguyên nhân và xử lý lại.
 */
public record RejectImportReceiptRequest(
        @NotBlank(message = "Lý do từ chối không được để trống.")
        @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự.")
        String reason
) {
}
