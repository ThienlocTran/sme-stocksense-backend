package com.smartflow.smestocksensebackend.excelimport;

public enum ExcelImportSheetName {
    HUONG_DAN("00_HuongDan"),
    SAN_PHAM("01_SanPham"),
    TON_DAU_KY("02_TonDauKy"),
    DANH_MUC_THAM_CHIEU("03_DanhMuc_ThamChieu"),
    KHO_THAM_CHIEU("04_Kho_ThamChieu"),
    GIA_TRI_HOP_LE("05_GiaTri_HopLe"),
    QUY_TAC_KIEM_TRA("06_QuyTac_KiemTra");

    private final String sheetName;

    ExcelImportSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getSheetName() {
        return sheetName;
    }
}
