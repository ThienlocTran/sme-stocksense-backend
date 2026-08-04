package com.smartflow.smestocksensebackend.dto.employee;

import com.smartflow.smestocksensebackend.entity.Gender;
import java.time.LocalDate;

public record ProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        String avatarUrl,
        Gender gender,
        LocalDate dateOfBirth
) {
}
