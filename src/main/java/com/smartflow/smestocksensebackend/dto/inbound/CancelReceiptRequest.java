package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.Size;

public record CancelReceiptRequest(
        @Size(max = 500, message = "Ly do huy khong duoc vuot qua 500 ky tu.")
        String reason
) {
}
