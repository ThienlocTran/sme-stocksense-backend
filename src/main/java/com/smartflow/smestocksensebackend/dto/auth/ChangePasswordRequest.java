package com.smartflow.smestocksensebackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mat khau hien tai khong duoc de trong.")
        String currentPassword,

        @NotBlank(message = "Mat khau moi khong duoc de trong.")
        @Size(min = 8, message = "Mat khau moi phai co it nhat 8 ky tu.")
        String newPassword,

        @NotBlank(message = "Xac nhan mat khau khong duoc de trong.")
        String confirmPassword
) {
}
