package com.smartflow.smestocksensebackend.excelimport;

import java.util.List;

public final class ExcelImportTemplateConstants {
    public static final String WORKBOOK_NAME = "SME_StockSense_Import_Template_v1.xlsx";

    public static final List<String> OFFICIAL_WORKBOOK_SHEETS = List.of(
            ExcelImportSheetName.SAN_PHAM.getSheetName(),
            ExcelImportSheetName.TON_DAU_KY.getSheetName()
    );

    public static final List<String> PRODUCT_HEADERS = ExcelImportColumn.productColumns()
            .stream()
            .map(ExcelImportColumn::getHeader)
            .toList();

    public static final List<String> OPENING_STOCK_HEADERS = ExcelImportColumn.openingStockColumns()
            .stream()
            .map(ExcelImportColumn::getHeader)
            .toList();

    private ExcelImportTemplateConstants() {
    }
}
