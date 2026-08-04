package com.smartflow.smestocksensebackend.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        Long employeeId,
        String fullName,
        String email,
        String role,
        String status,
        String avatarUrl
) {
}
