package com.smartflow.smestocksensebackend.excelimport;

import java.util.List;

public enum ExcelImportColumn {
    PRODUCT_CODE(ExcelImportSheetName.SAN_PHAM, "ma_san_pham", true),
    PRODUCT_NAME(ExcelImportSheetName.SAN_PHAM, "ten_san_pham", true),
    SKU(ExcelImportSheetName.SAN_PHAM, "sku", false),
    BARCODE(ExcelImportSheetName.SAN_PHAM, "ma_vach", false),
    UNIT(ExcelImportSheetName.SAN_PHAM, "don_vi_tinh", true),
    CATEGORY_CODE(ExcelImportSheetName.SAN_PHAM, "ma_danh_muc", true),
    SUPPLIER_CODE(ExcelImportSheetName.SAN_PHAM, "ma_nha_cung_cap", false),
    SALE_PRICE(ExcelImportSheetName.SAN_PHAM, "gia_ban", false),
    DEFAULT_MIN_STOCK(ExcelImportSheetName.SAN_PHAM, "ton_toi_thieu_mac_dinh", true),
    UNIT_VOLUME(ExcelImportSheetName.SAN_PHAM, "the_tich_don_vi_m3", false),
    LEAD_TIME_DAYS(ExcelImportSheetName.SAN_PHAM, "thoi_gian_giao_hang", false),
    PRODUCT_STATUS(ExcelImportSheetName.SAN_PHAM, "trang_thai", false),
    WAREHOUSE_CODE(ExcelImportSheetName.TON_DAU_KY, "ma_kho", true),
    OPENING_PRODUCT_CODE(ExcelImportSheetName.TON_DAU_KY, "ma_san_pham", true),
    OPENING_QUANTITY(ExcelImportSheetName.TON_DAU_KY, "so_luong_ton", true);

    private final ExcelImportSheetName sheetName;
    private final String header;
    private final boolean required;

    ExcelImportColumn(ExcelImportSheetName sheetName, String header, boolean required) {
        this.sheetName = sheetName;
        this.header = header;
        this.required = required;
    }

    public ExcelImportSheetName getSheetName() {
        return sheetName;
    }

    public String getHeader() {
        return header;
    }

    public boolean isRequired() {
        return required;
    }

    public static List<ExcelImportColumn> productColumns() {
        return List.of(
                PRODUCT_CODE,
                PRODUCT_NAME,
                SKU,
                BARCODE,
                UNIT,
                CATEGORY_CODE,
                SUPPLIER_CODE,
                SALE_PRICE,
                DEFAULT_MIN_STOCK,
                UNIT_VOLUME,
                LEAD_TIME_DAYS,
                PRODUCT_STATUS
        );
    }

    public static List<ExcelImportColumn> openingStockColumns() {
        return List.of(
                WAREHOUSE_CODE,
                OPENING_PRODUCT_CODE,
                OPENING_QUANTITY
        );
    }
}
