package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO nhận yêu cầu ghi nhận hàng về thực tế cho phiếu nhập kho.
 */
public record ImportReceiptArrivalRequest(
        @NotNull(message = "Ngày hàng về thực tế không được để trống.")
        LocalDateTime actualArrivalDate
) {
}
