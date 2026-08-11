package com.smartflow.smestocksensebackend.excelimport;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelImportTemplateServiceTest {

    private final ExcelImportTemplateService excelImportTemplateService = new ExcelImportTemplateService();

    @Test
    void generateTemplate_shouldCreateCleanBusinessWorkbook() throws Exception {
        byte[] bytes = excelImportTemplateService.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(sheetNames(workbook)).containsExactlyElementsOf(ExcelImportTemplateConstants.OFFICIAL_WORKBOOK_SHEETS);
            assertHeader(workbook.getSheet(ExcelImportSheetName.SAN_PHAM.getSheetName()).getRow(0),
                    ExcelImportTemplateConstants.PRODUCT_HEADERS);
            assertHeader(workbook.getSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName()).getRow(0),
                    ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);
            assertThat(workbook.getSheet(ExcelImportSheetName.SAN_PHAM.getSheetName()).getLastRowNum()).isZero();
            assertThat(workbook.getSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName()).getLastRowNum()).isZero();
        }
    }

    private List<String> sheetNames(Workbook workbook) {
        return java.util.stream.IntStream.range(0, workbook.getNumberOfSheets())
                .mapToObj(workbook::getSheetName)
                .toList();
    }

    private void assertHeader(Row row, List<String> expectedHeaders) {
        List<String> actualHeaders = java.util.stream.IntStream.range(0, expectedHeaders.size())
                .mapToObj(index -> row.getCell(index).getStringCellValue())
                .toList();
        assertThat(actualHeaders).containsExactlyElementsOf(expectedHeaders);
    }
}
