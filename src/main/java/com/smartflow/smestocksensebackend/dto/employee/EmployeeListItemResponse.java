package com.smartflow.smestocksensebackend.dto.employee;

import com.smartflow.smestocksensebackend.entity.Employee;

import java.time.LocalDateTime;

public record EmployeeListItemResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Long roleId,
        String roleCode,
        String roleName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static EmployeeListItemResponse from(Employee employee) {
        return new EmployeeListItemResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRole().getId(),
                employee.getRole().getCode().name(),
                employee.getRole().getName(),
                employee.getStatus().name(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
