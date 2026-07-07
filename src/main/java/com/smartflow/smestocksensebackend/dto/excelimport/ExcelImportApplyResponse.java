package com.smartflow.smestocksensebackend.dto.excelimport;

import com.smartflow.smestocksensebackend.entity.ExcelImport;

import java.time.LocalDateTime;

public record ExcelImportApplyResponse(
        Long importId,
        String status,
        Integer totalRows,
        Integer validRows,
        Integer errorRows,
        LocalDateTime completedAt,
        String message
) {

    public static ExcelImportApplyResponse from(ExcelImport excelImport) {
        return new ExcelImportApplyResponse(
                excelImport.getId(),
                excelImport.getStatus().name(),
                excelImport.getTotalRows(),
                excelImport.getValidRows(),
                excelImport.getErrorRows(),
                excelImport.getCompletedAt(),
                "Lan import da duoc ap dung thanh cong."
        );
    }
}
