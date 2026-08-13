package com.smartflow.smestocksensebackend.dto.excelimport;

import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;

import java.time.LocalDateTime;

public record ExcelImportUploadResponse(
        Long id,
        String tenFile,
        String loaiImport,
        String trangThai,
        Integer tongSoDong,
        Integer soDongHopLe,
        Integer soDongLoi,
        LocalDateTime createdAt,
        boolean valid,
        boolean canConfirm
) {

    public ExcelImportUploadResponse(
            Long id,
            String tenFile,
            String loaiImport,
            String trangThai,
            Integer tongSoDong,
            Integer soDongHopLe,
            Integer soDongLoi,
            LocalDateTime createdAt
    ) {
        this(id, tenFile, loaiImport, trangThai, tongSoDong, soDongHopLe, soDongLoi, createdAt,
                isValid(trangThai, soDongLoi),
                canConfirm(trangThai, soDongLoi));
    }

    public static ExcelImportUploadResponse from(ExcelImport excelImport) {
        String status = excelImport.getStatus().name();
        return new ExcelImportUploadResponse(
                excelImport.getId(),
                excelImport.getFileName(),
                excelImport.getImportType(),
                status,
                excelImport.getTotalRows(),
                excelImport.getValidRows(),
                excelImport.getErrorRows(),
                excelImport.getCreatedAt(),
                isValid(status, excelImport.getErrorRows()),
                canConfirm(status, excelImport.getErrorRows())
        );
    }

    private static boolean isValid(String status, Integer errorRows) {
        return ExcelImportStatus.SAN_SANG_IMPORT.name().equals(status) && errorRows != null && errorRows == 0;
    }

    private static boolean canConfirm(String status, Integer errorRows) {
        return isValid(status, errorRows);
    }
}
