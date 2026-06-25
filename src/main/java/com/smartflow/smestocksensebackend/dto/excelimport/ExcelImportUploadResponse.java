package com.smartflow.smestocksensebackend.dto.excelimport;

import com.smartflow.smestocksensebackend.entity.ExcelImport;

import java.time.LocalDateTime;

public record ExcelImportUploadResponse(
        Long id,
        String tenFile,
        String loaiImport,
        String trangThai,
        Integer tongSoDong,
        Integer soDongHopLe,
        Integer soDongLoi,
        LocalDateTime createdAt
) {

    public static ExcelImportUploadResponse from(ExcelImport excelImport) {
        return new ExcelImportUploadResponse(
                excelImport.getId(),
                excelImport.getFileName(),
                excelImport.getImportType(),
                excelImport.getStatus().name(),
                excelImport.getTotalRows(),
                excelImport.getValidRows(),
                excelImport.getErrorRows(),
                excelImport.getCreatedAt()
        );
    }
}
