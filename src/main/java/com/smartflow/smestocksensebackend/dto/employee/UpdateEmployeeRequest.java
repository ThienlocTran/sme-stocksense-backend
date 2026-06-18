package com.smartflow.smestocksensebackend.dto.employee;

import jakarta.validation.constraints.NotBlank;

public record UpdateEmployeeRequest(
        @NotBlank(message = "Ho ten khong duoc de trong.")
        String fullName,

        @NotBlank(message = "Email khong duoc de trong.")
        String email,

        String phoneNumber,

        @NotBlank(message = "roleCode khong duoc de trong.")
        String roleCode,

        @NotBlank(message = "status khong duoc de trong.")
        String status
) {
}
