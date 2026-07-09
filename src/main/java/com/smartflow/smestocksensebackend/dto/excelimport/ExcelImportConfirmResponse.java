package com.smartflow.smestocksensebackend.dto.excelimport;

import com.smartflow.smestocksensebackend.entity.ExcelImport;

public record ExcelImportConfirmResponse(
        Long importId,
        String status,
        Integer totalRows,
        Integer validRows,
        Integer errorRows,
        String message
) {

    public static ExcelImportConfirmResponse from(ExcelImport excelImport) {
        return new ExcelImportConfirmResponse(
                excelImport.getId(),
                excelImport.getStatus().name(),
                excelImport.getTotalRows(),
                excelImport.getValidRows(),
                excelImport.getErrorRows(),
                "Lan import da san sang de thuc hien buoc tiep theo."
        );
    }
}
