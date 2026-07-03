package com.smartflow.smestocksensebackend.dto.excelimport;

import java.util.List;

public record ExcelImportValidationResponse(
        boolean valid,
        String loaiImport,
        Integer tongSoDong,
        Integer soDongHopLe,
        Integer soDongLoi,
        List<ExcelImportValidationErrorResponse> errors
) {
}
