package com.smartflow.smestocksensebackend.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank(message = "Ho ten khong duoc de trong.")
        String fullName,

        @NotBlank(message = "Email khong duoc de trong.")
        String email,

        String phoneNumber,

        @NotBlank(message = "Mat khau khong duoc de trong.")
        @Size(min = 8, message = "Mat khau phai co it nhat 8 ky tu.")
        String password,

        @NotBlank(message = "roleCode khong duoc de trong.")
        String roleCode,

        String status
) {
}
