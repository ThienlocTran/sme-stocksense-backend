package com.smartflow.smestocksensebackend.dto.excelimport;

public record ExcelImportValidationErrorResponse(
        String sheetName,
        Integer rowNumber,
        String columnName,
        String rawValue,
        String message,
        String suggestion
) {
}
