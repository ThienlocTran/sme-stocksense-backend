package com.smartflow.smestocksensebackend.dto.employee;

import com.smartflow.smestocksensebackend.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;


import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank(message = "Họ tên không được để trống")
        String fullName,
        
        String phone,
        
        Gender gender,
        
        @Past(message = "Ngày sinh phải ở trong quá khứ")
        LocalDate dateOfBirth
) {
}
