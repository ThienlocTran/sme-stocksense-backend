package com.smartflow.smestocksensebackend.dto.excelimport;

import com.smartflow.smestocksensebackend.entity.ExcelImportError;

public record ExcelImportErrorResponse(
        Long id,
        Long importId,
        Integer rowNumber,
        String columnName,
        String originalValue,
        String message,
        String suggestion
) {

    public static ExcelImportErrorResponse from(ExcelImportError error) {
        return new ExcelImportErrorResponse(
                error.getId(),
                error.getExcelImport().getId(),
                error.getRowNumber(),
                error.getColumnName(),
                error.getOriginalValue(),
                error.getMessage(),
                error.getSuggestion()
        );
    }
}
