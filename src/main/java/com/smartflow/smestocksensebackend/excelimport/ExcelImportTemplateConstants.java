package com.smartflow.smestocksensebackend.excelimport;

import java.util.List;

public final class ExcelImportTemplateConstants {
    public static final String WORKBOOK_NAME = "SME_StockSense_Import_Template_v1.xlsx";

    public static final List<String> OFFICIAL_WORKBOOK_SHEETS = List.of(
            ExcelImportSheetName.HUONG_DAN.getSheetName(),
            ExcelImportSheetName.SAN_PHAM.getSheetName(),
            ExcelImportSheetName.TON_DAU_KY.getSheetName(),
            ExcelImportSheetName.DANH_MUC_THAM_CHIEU.getSheetName(),
            ExcelImportSheetName.KHO_THAM_CHIEU.getSheetName(),
            ExcelImportSheetName.GIA_TRI_HOP_LE.getSheetName(),
            ExcelImportSheetName.QUY_TAC_KIEM_TRA.getSheetName()
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
