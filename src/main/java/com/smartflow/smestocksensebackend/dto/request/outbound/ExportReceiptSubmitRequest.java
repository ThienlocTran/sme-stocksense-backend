package com.smartflow.smestocksensebackend.dto.request.outbound;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload dùng để Gửi Phiếu Xuất Duyệt (T118).
 * Yêu cầu truyền lên version để xử lý Optimistic Locking.
 */
@Getter
@Setter
public class ExportReceiptSubmitRequest {

    @NotNull(message = "Phiên bản (version) không được để trống")
    private Long version;
}
