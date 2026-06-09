package com.smartflow.smestocksensebackend.dto.employee;

import org.springframework.data.domain.Page;

import java.util.List;

public record EmployeePageResponse(
        List<EmployeeListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static EmployeePageResponse from(Page<EmployeeListItemResponse> employeePage) {
        return new EmployeePageResponse(
                employeePage.getContent(),
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements(),
                employeePage.getTotalPages()
        );
    }
}
