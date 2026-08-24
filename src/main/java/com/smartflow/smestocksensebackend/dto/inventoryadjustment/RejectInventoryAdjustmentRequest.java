package com.smartflow.smestocksensebackend.dto.inventoryadjustment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectInventoryAdjustmentRequest(
        @NotBlank(message = "Ly do tu choi khong duoc de trong.")
        @Size(max = 500, message = "Ly do tu choi khong duoc vuot qua 500 ky tu.")
        String rejectionReason
) {
}
