package com.smartflow.smestocksensebackend.dto.auth;

public record LoginResponse(
        Long employeeId,
        String fullName,
        String email,
        String role,
        String status
) {
}
