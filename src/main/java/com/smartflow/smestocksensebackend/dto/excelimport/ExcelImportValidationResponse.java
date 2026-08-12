package com.smartflow.smestocksensebackend.dto.excelimport;

import java.util.List;

public record ExcelImportValidationResponse(
        boolean valid,
        String loaiImport,
        Integer tongSoDong,
        Integer soDongHopLe,
        Integer soDongLoi,
        List<ExcelImportValidationErrorResponse> errors,
        boolean canConfirm
) {

    public ExcelImportValidationResponse(
            boolean valid,
            String loaiImport,
            Integer tongSoDong,
            Integer soDongHopLe,
            Integer soDongLoi,
            List<ExcelImportValidationErrorResponse> errors
    ) {
        this(valid, loaiImport, tongSoDong, soDongHopLe, soDongLoi, errors,
                valid && soDongLoi != null && soDongLoi == 0);
    }
}
