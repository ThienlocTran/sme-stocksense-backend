package com.smartflow.smestocksensebackend.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Mat khau moi khong duoc de trong.")
        @Size(min = 8, message = "Mat khau moi phai co it nhat 8 ky tu.")
        String newPassword
) {
}
