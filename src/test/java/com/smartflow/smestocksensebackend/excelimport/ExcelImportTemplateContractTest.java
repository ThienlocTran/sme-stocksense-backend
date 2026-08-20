package com.smartflow.smestocksensebackend.excelimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelImportTemplateContractTest {

    @Test
    void workbookSheets_shouldMatchOfficialScope() {
        assertEquals(List.of(
                "01_SanPham",
                "02_TonDauKy"
        ), ExcelImportTemplateConstants.OFFICIAL_WORKBOOK_SHEETS);
    }

    @Test
    void productHeaders_shouldMatchOfficialMvpContract() {
        assertEquals(List.of(
                "ma_san_pham",
                "ten_san_pham",
                "sku",
                "ma_vach",
                "don_vi_tinh",
                "ma_danh_muc",
                "gia_ban",
                "the_tich_don_vi_m3",
                "trang_thai"
        ), ExcelImportTemplateConstants.PRODUCT_HEADERS);
    }

    @Test
    void openingStockHeaders_shouldMatchOfficialMvpContract() {
        assertEquals(List.of(
                "ma_kho",
                "ma_san_pham",
                "so_luong_ton"
        ), ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);
    }

    @Test
    void importModes_shouldStayTemplateOnly() {
        assertEquals(List.of(
                "PRODUCT_ONLY",
                "PRODUCT_WITH_OPENING_STOCK"
        ), List.of(
                ExcelImportMode.PRODUCT_ONLY.name(),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name()
        ));
    }
}
