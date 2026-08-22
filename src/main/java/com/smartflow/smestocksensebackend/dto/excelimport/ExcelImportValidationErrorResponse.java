package com.smartflow.smestocksensebackend.dto.excelimport;

public record ExcelImportValidationErrorResponse(
        String sheetName,
        Integer rowNumber,
        String columnName,
        String rawValue,
        String message,
        String suggestion,
        String errorCode
) {
    public ExcelImportValidationErrorResponse(
            String sheetName,
            Integer rowNumber,
            String columnName,
            String rawValue,
            String message,
            String suggestion
    ) {
        this(sheetName, rowNumber, columnName, rawValue, message, suggestion, null);
    }
}
