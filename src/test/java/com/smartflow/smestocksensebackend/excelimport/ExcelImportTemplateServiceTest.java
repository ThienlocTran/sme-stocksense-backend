package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.CategoryStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportTemplateServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private ExcelImportTemplateService excelImportTemplateService;

    @Test
    void generateTemplate_shouldCreateOfficialWorkbookStructure() throws Exception {
        Category category = new Category();
        category.setCode("DM01");
        category.setName("Danh mục 01");
        category.setStatus(CategoryStatus.HOAT_DONG);

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("KHO01");
        warehouse.setName("Kho 01");
        warehouse.setStatus(WarehouseStatus.HOAT_DONG);

        when(categoryRepository.findAll(Sort.by("code"))).thenReturn(List.of(category));
        when(warehouseRepository.findAll(Sort.by("code"))).thenReturn(List.of(warehouse));

        byte[] bytes = excelImportTemplateService.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(7);
            assertThat(sheetNames(workbook)).containsExactlyElementsOf(ExcelImportTemplateConstants.OFFICIAL_WORKBOOK_SHEETS);
            assertHeader(workbook.getSheet(ExcelImportSheetName.SAN_PHAM.getSheetName()).getRow(0),
                    ExcelImportTemplateConstants.PRODUCT_HEADERS);
            assertHeader(workbook.getSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName()).getRow(0),
                    ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);
            assertThat(workbook.getSheet("07_Source_Research")).isNull();
            assertThat(workbook.getSheet(ExcelImportSheetName.DANH_MUC_THAM_CHIEU.getSheetName())
                    .getRow(1).getCell(0).getStringCellValue()).isEqualTo("DM01");
            assertThat(workbook.getSheet(ExcelImportSheetName.KHO_THAM_CHIEU.getSheetName())
                    .getRow(1).getCell(0).getStringCellValue()).isEqualTo("KHO01");
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
