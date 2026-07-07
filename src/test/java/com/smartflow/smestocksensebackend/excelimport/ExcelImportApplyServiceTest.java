package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportApplyResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportErrorRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportApplyServiceTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private ExcelImportRepository excelImportRepository;

    @Mock
    private ExcelImportErrorRepository excelImportErrorRepository;

    @Mock
    private ExcelImportValidationService excelImportValidationService;

    @Mock
    private ExcelImportChecksumService excelImportChecksumService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @InjectMocks
    private ExcelImportApplyService applyService;

    @Test
    void apply_missingImportShouldThrowNotFound() {
        when(excelImportRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applyService.apply(404L, file(productWorkbook())))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lan import khong ton tai.");
    }

    @Test
    void apply_rejectsStatusNotConfirmed() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_ONLY);
        excelImport.setStatus(ExcelImportStatus.SAN_SANG_IMPORT);
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));

        assertThatThrownBy(() -> applyService.apply(99L, file(productWorkbook())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lan import chua duoc xac nhan hoac da duoc import.");
    }

    @Test
    void apply_rejectsAlreadyImported() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_ONLY);
        excelImport.setStatus(ExcelImportStatus.DA_IMPORT);
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));

        assertThatThrownBy(() -> applyService.apply(99L, file(productWorkbook())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lan import chua duoc xac nhan hoac da duoc import.");
    }

    @Test
    void apply_rejectsMissingChecksum() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_ONLY);
        excelImport.setChecksumFileSha256(null);
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));

        assertThatThrownBy(() -> applyService.apply(99L, file(productWorkbook())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Lan import chua co checksum file da validate.");
    }

    @Test
    void apply_rejectsChecksumMismatch() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_ONLY);
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));
        when(excelImportChecksumService.sha256(any())).thenReturn("different");

        assertThatThrownBy(() -> applyService.apply(99L, file(productWorkbook())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File Excel khong khop voi file da validate.");
    }

    @Test
    void apply_revalidatesBeforeOfficialWrites() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_ONLY);
        MockMultipartFile file = file(productWorkbook());
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));
        when(excelImportChecksumService.sha256(file)).thenReturn("checksum");
        when(excelImportErrorRepository.existsByExcelImportId(99L)).thenReturn(false);
        when(excelImportValidationService.validate(file, ExcelImportMode.PRODUCT_ONLY.name(), null))
                .thenReturn(new ExcelImportValidationResponse(false, ExcelImportMode.PRODUCT_ONLY.name(), 1, 0, 1, List.of()));

        assertThatThrownBy(() -> applyService.apply(99L, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File Excel khong con hop le de import.");

        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(inventoryLevelRepository, never()).saveAndFlush(any(InventoryLevel.class));
        verify(excelImportRepository, never()).save(excelImport);
    }

    @Test
    void apply_productOnlyUpsertsProductAndDoesNotUpdateInventory() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_ONLY);
        MockMultipartFile file = file(productWorkbook());
        Category category = category();
        Product product = product(10L, "P01");
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));
        when(excelImportChecksumService.sha256(file)).thenReturn("checksum");
        when(excelImportErrorRepository.existsByExcelImportId(99L)).thenReturn(false);
        when(excelImportValidationService.validate(file, ExcelImportMode.PRODUCT_ONLY.name(), null))
                .thenReturn(new ExcelImportValidationResponse(true, ExcelImportMode.PRODUCT_ONLY.name(), 1, 1, 0, List.of()));
        when(categoryRepository.findByNormalizedCode("CAT01")).thenReturn(Optional.of(category));
        when(productRepository.findByCode("P01")).thenReturn(Optional.of(product));
        when(excelImportRepository.save(excelImport)).thenReturn(excelImport);

        ExcelImportApplyResponse response = applyService.apply(99L, file);

        assertThat(product.getName()).isEqualTo("San pham moi");
        assertThat(product.getSku()).isEqualTo("SKU01");
        assertThat(product.getBarcode()).isEqualTo("BAR01");
        assertThat(product.getUnit()).isEqualTo("Cai");
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(product.getMinStock()).isEqualTo(1);
        assertThat(product.getMaxStock()).isEqualTo(5);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.HOAT_DONG);
        assertThat(response.status()).isEqualTo(ExcelImportStatus.DA_IMPORT.name());
        assertThat(excelImport.getCompletedAt()).isNotNull();
        verify(productRepository).saveAndFlush(product);
        verify(inventoryLevelRepository, never()).saveAndFlush(any(InventoryLevel.class));
    }

    @Test
    void apply_openingStockSetsExactQuantityAfterProductRows() {
        ExcelImport excelImport = readyImport(99L, ExcelImportMode.PRODUCT_WITH_OPENING_STOCK);
        MockMultipartFile file = file(openingWorkbook());
        Category category = category();
        Product product = product(10L, "P01");
        Warehouse warehouse = warehouse();
        InventoryLevel level = new InventoryLevel();
        level.setProduct(product);
        level.setWarehouse(warehouse);
        level.setQuantity(5);
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));
        when(excelImportChecksumService.sha256(file)).thenReturn("checksum");
        when(excelImportErrorRepository.existsByExcelImportId(99L)).thenReturn(false);
        when(excelImportValidationService.validate(file, ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(), null))
                .thenReturn(new ExcelImportValidationResponse(true, ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(), 2, 2, 0, List.of()));
        when(categoryRepository.findByNormalizedCode("CAT01")).thenReturn(Optional.of(category));
        when(productRepository.findByCode("P01")).thenReturn(Optional.of(product));
        when(warehouseRepository.findByCodeIgnoreCase("WH01")).thenReturn(Optional.of(warehouse));
        when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(10L, 20L)).thenReturn(Optional.of(level));
        when(excelImportRepository.save(excelImport)).thenReturn(excelImport);

        applyService.apply(99L, file);

        assertThat(level.getQuantity()).isEqualTo(12);
        assertThat(excelImport.getStatus()).isEqualTo(ExcelImportStatus.DA_IMPORT);
        assertThat(excelImport.getCompletedAt()).isNotNull();
        verify(productRepository).saveAndFlush(product);
        verify(inventoryLevelRepository).saveAndFlush(level);
    }

    private ExcelImport readyImport(Long id, ExcelImportMode mode) {
        ExcelImport excelImport = new ExcelImport();
        excelImport.setId(id);
        excelImport.setImportType(mode.name());
        excelImport.setStatus(ExcelImportStatus.DA_XAC_NHAN);
        excelImport.setTotalRows(mode == ExcelImportMode.PRODUCT_ONLY ? 1 : 2);
        excelImport.setValidRows(mode == ExcelImportMode.PRODUCT_ONLY ? 1 : 2);
        excelImport.setErrorRows(0);
        excelImport.setChecksumFileSha256("checksum");
        return excelImport;
    }

    private Category category() {
        Category category = new Category();
        category.setId(30L);
        category.setCode("CAT01");
        return category;
    }

    private Product product(Long id, String code) {
        Product product = new Product();
        product.setId(id);
        product.setCode(code);
        product.setName("Cu");
        product.setUnit("Cai");
        return product;
    }

    private Warehouse warehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(20L);
        warehouse.setCode("WH01");
        warehouse.setName("Kho 1");
        return warehouse;
    }

    private XSSFWorkbook productWorkbook() {
        return workbook(
                List.of(List.of("P01", "San pham moi", "SKU01", "BAR01", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                null
        );
    }

    private XSSFWorkbook openingWorkbook() {
        return workbook(
                List.of(List.of("P01", "San pham moi", "SKU01", "BAR01", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                List.of(List.of("WH01", "P01", "12"))
        );
    }

    private MockMultipartFile file(XSSFWorkbook workbook) {
        try (workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new MockMultipartFile("file", "sample.xlsx", XLSX, out.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private XSSFWorkbook workbook(List<List<String>> productRows, List<List<String>> openingRows) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet productSheet = workbook.createSheet(ExcelImportSheetName.SAN_PHAM.getSheetName());
        writeRow(productSheet, 0, ExcelImportTemplateConstants.PRODUCT_HEADERS);
        for (int index = 0; index < productRows.size(); index++) {
            writeRow(productSheet, index + 1, productRows.get(index));
        }
        if (openingRows != null) {
            Sheet openingSheet = workbook.createSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName());
            writeRow(openingSheet, 0, ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);
            for (int index = 0; index < openingRows.size(); index++) {
                writeRow(openingSheet, index + 1, openingRows.get(index));
            }
        }
        return workbook;
    }

    private void writeRow(Sheet sheet, int rowIndex, List<String> values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }
}
