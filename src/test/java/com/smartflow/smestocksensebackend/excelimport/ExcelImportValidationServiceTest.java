package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportErrorResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationErrorResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportError;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportErrorRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportValidationServiceTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ExcelImportRepository excelImportRepository;

    @Mock
    private ExcelImportErrorRepository excelImportErrorRepository;

    @InjectMocks
    private ExcelImportValidationService validationService;

    @Test
    void validate_validProductOnlyWorkbookReturnsValidTrue() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);
        when(productRepository.existsBySkuIgnoreCase("SKU01")).thenReturn(false);
        when(productRepository.existsByBarcodeIgnoreCase("BAR01")).thenReturn(false);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "San pham 1", "SKU01", "BAR01", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isTrue();
        assertThat(response.tongSoDong()).isEqualTo(1);
        assertThat(response.soDongHopLe()).isEqualTo(1);
        assertThat(response.soDongLoi()).isZero();
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void validate_missingProductSheetReturnsValidationError() throws Exception {
        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(null, null, null, List.of(List.of("placeholder")))),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> "Thiếu sheet 01_SanPham.".equals(error.message()));
    }

    @Test
    void validate_wrongProductHeaderReturnsValidationError() throws Exception {
        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        List.of("sai_cot", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", "ma_danh_muc", "gia_ban", "ton_toi_thieu", "ton_toi_da", "trang_thai"),
                        List.of(List.of("P01", "SP", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.message().contains("Header sheet 01_SanPham"));
    }

    @Test
    void validate_missingRequiredProductFieldsReturnsValidationErrors() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("", "", "", "", "", "CAT01", "", "", "", "")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).extracting(ExcelImportValidationErrorResponse::columnName)
                .contains("ma_san_pham", "ten_san_pham", "don_vi_tinh");
    }

    @Test
    void validate_duplicateProductCodeReturnsValidationError() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(
                                List.of("P01", "SP1", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG"),
                                List.of("P01", "SP2", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")
                        ),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.message().contains("Mã sản phẩm bị trùng trong file."));
    }

    @Test
    void validate_invalidCategoryReturnsValidationError() throws Exception {
        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "", "", "Cai", "CAT99", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.message().equals("Danh mục không tồn tại."));
    }

    @Test
    void validate_negativePriceAndStockRangeAndInvalidStatusReturnsValidationErrors() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "", "", "Cai", "CAT01", "-10", "8", "5", "BOGUS")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).extracting(ExcelImportValidationErrorResponse::message)
                .contains("Giá trị không được âm.", "Tồn tối thiểu phải nhỏ hơn hoặc bằng tồn tối đa.", "Trạng thái sản phẩm không hợp lệ.");
    }

    @Test
    void validate_validProductWithOpeningStockWorkbookReturnsValidTrue() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);
        when(productRepository.existsBySkuIgnoreCase("SKU01")).thenReturn(false);
        when(productRepository.existsByBarcodeIgnoreCase("BAR01")).thenReturn(false);
        when(warehouseRepository.existsByCodeIgnoreCase("WH01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "SKU01", "BAR01", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        List.of(List.of("WH01", "P01", "12")),
                        null
                )),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                null
        );

        assertThat(response.valid()).isTrue();
        assertThat(response.tongSoDong()).isEqualTo(2);
        assertThat(response.soDongHopLe()).isEqualTo(2);
        assertThat(response.soDongLoi()).isZero();
    }

    @Test
    void validate_missingOpeningSheetReturnsValidationError() throws Exception {
        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> "Thiếu sheet 02_TonDauKy.".equals(error.message()));
    }

    @Test
    void validate_duplicateOpeningStockPairReturnsValidationError() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);
        when(warehouseRepository.existsByCodeIgnoreCase("WH01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        List.of(
                                List.of("WH01", "P01", "12"),
                                List.of("WH01", "P01", "13")
                        ),
                        null
                )),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.message().contains("Cặp mã kho và mã sản phẩm bị trùng trong file."));
    }

    @Test
    void validate_invalidWarehouseReturnsValidationError() throws Exception {
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        List.of(List.of("WH99", "P01", "12")),
                        null
                )),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.message().equals("Kho hàng không tồn tại."));
    }

    @Test
    void validate_unknownOpeningStockProductReturnsValidationErrorWhenNotInDbAndNotInFile() throws Exception {
        when(warehouseRepository.existsByCodeIgnoreCase("WH01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        null,
                        null,
                        List.of(List.of("WH01", "P99", "12")),
                        null
                )),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.message().equals("Sản phẩm không tồn tại."));
    }

    @Test
    void validate_missingFileShouldReturnBadRequest() {
        assertThatThrownBy(() -> validationService.validate(null, ExcelImportMode.PRODUCT_ONLY.name(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("file is required.");
    }

    @Test
    void validate_emptyFileShouldReturnBadRequest() {
        assertThatThrownBy(() -> validationService.validate(
                new MockMultipartFile("file", "empty.xlsx", XLSX, new byte[]{}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("file must not be empty.");
    }

    @Test
    void validate_nonXlsxFileShouldReturnBadRequest() {
        assertThatThrownBy(() -> validationService.validate(
                new MockMultipartFile("file", "bad.csv", "text/csv", new byte[]{1}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("file must be .xlsx.");
    }

    @Test
    void validate_unsupportedImportModeShouldReturnBadRequest() {
        assertThatThrownBy(() -> validationService.validate(
                new MockMultipartFile("file", "bad.xlsx", XLSX, new byte[]{1, 2, 3}),
                "INVALID",
                null
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("loaiImport is not supported.");
    }

    @Test
    void validate_invalidWarehouseIdAddsValidationError() throws Exception {
        when(warehouseRepository.existsById(404L)).thenReturn(false);
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);
        when(warehouseRepository.existsByCodeIgnoreCase("WH01")).thenReturn(true);

        ExcelImportValidationResponse response = validationService.validate(
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "SP1", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        List.of(List.of("WH01", "P01", "12")),
                        null
                )),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                404L
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> "Kho hàng không tồn tại.".equals(error.message()));
    }

    @Test
    void validateAndPersistErrors_invalidWorkbookSavesCountersAndErrors() throws Exception {
        ExcelImport excelImport = new ExcelImport();
        excelImport.setId(99L);
        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));

        ExcelImportValidationResponse response = validationService.validateAndPersistErrors(
                99L,
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("", "San pham loi", "", "", "Cai", "CAT99", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(response.valid()).isFalse();
        assertThat(excelImport.getStatus()).isEqualTo(ExcelImportStatus.CO_LOI);
        assertThat(excelImport.getTotalRows()).isEqualTo(1);
        assertThat(excelImport.getValidRows()).isZero();
        assertThat(excelImport.getErrorRows()).isEqualTo(1);

        verify(excelImportErrorRepository).deleteByExcelImportId(99L);
        verify(excelImportRepository).save(excelImport);
        verify(excelImportErrorRepository).saveAll(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void validateAndPersistErrors_validWorkbookClearsOldErrorsAndMarksReady() throws Exception {
        ExcelImport excelImport = new ExcelImport();
        excelImport.setId(100L);
        when(excelImportRepository.findById(100L)).thenReturn(Optional.of(excelImport));
        when(categoryRepository.existsByNormalizedCode("CAT01")).thenReturn(true);

        validationService.validateAndPersistErrors(
                100L,
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("P01", "San pham 1", "", "", "Cai", "CAT01", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        assertThat(excelImport.getStatus()).isEqualTo(ExcelImportStatus.SAN_SANG_IMPORT);
        assertThat(excelImport.getTotalRows()).isEqualTo(1);
        assertThat(excelImport.getValidRows()).isEqualTo(1);
        assertThat(excelImport.getErrorRows()).isZero();
        verify(excelImportErrorRepository).deleteByExcelImportId(100L);
        verify(excelImportErrorRepository, never()).saveAll(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    void validateAndPersistErrors_revalidationReplacesOldErrorsAndPersistsFields() throws Exception {
        ExcelImport excelImport = new ExcelImport();
        excelImport.setId(101L);
        when(excelImportRepository.findById(101L)).thenReturn(Optional.of(excelImport));

        validationService.validateAndPersistErrors(
                101L,
                xlsxFile(workbook(
                        ExcelImportTemplateConstants.PRODUCT_HEADERS,
                        List.of(List.of("", "San pham loi", "", "", "Cai", "CAT99", "10", "1", "5", "HOAT_DONG")),
                        null,
                        null
                )),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        org.mockito.ArgumentCaptor<Iterable<ExcelImportError>> errorsCaptor =
                org.mockito.ArgumentCaptor.forClass(Iterable.class);
        verify(excelImportErrorRepository).deleteByExcelImportId(101L);
        verify(excelImportErrorRepository).saveAll(errorsCaptor.capture());

        List<ExcelImportError> savedErrors = ((List<ExcelImportError>) errorsCaptor.getValue());
        assertThat(savedErrors).hasSize(2);
        assertThat(savedErrors).extracting(ExcelImportError::getExcelImport)
                .allSatisfy(savedImport -> assertThat(savedImport).isSameAs(excelImport));
        assertThat(savedErrors).extracting(ExcelImportError::getRowNumber)
                .containsExactly(2, 2);
        assertThat(savedErrors).extracting(ExcelImportError::getColumnName)
                .containsExactly("ma_san_pham", "ma_danh_muc");
        assertThat(savedErrors).extracting(ExcelImportError::getOriginalValue)
                .containsExactly("", "CAT99");
        assertThat(savedErrors).extracting(ExcelImportError::getMessage)
                .allSatisfy(message -> assertThat(message).isNotNull().isNotBlank());
        assertThat(excelImport.getTotalRows()).isEqualTo(1);
        assertThat(excelImport.getValidRows()).isZero();
        assertThat(excelImport.getErrorRows()).isEqualTo(1);
    }

    @Test
    void validateAndPersistErrors_missingImportShouldReturnNotFound() {
        when(excelImportRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.validateAndPersistErrors(
                404L,
                new MockMultipartFile("file", "sample.xlsx", XLSX, new byte[]{1}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lan import khong ton tai.");

        verify(excelImportErrorRepository, never()).deleteByExcelImportId(anyLong());
        verify(excelImportRepository, never()).save(any(ExcelImport.class));
        verify(excelImportErrorRepository, never()).saveAll(any());
    }

    @Test
    void listErrors_existingImportReturnsPersistedErrorsOnly() {
        ExcelImport excelImport = new ExcelImport();
        excelImport.setId(99L);
        excelImport.setStatus(ExcelImportStatus.CO_LOI);
        excelImport.setTotalRows(3);
        excelImport.setValidRows(1);
        excelImport.setErrorRows(2);

        ExcelImportError first = error(7L, excelImport, 2, "ma_san_pham");
        ExcelImportError second = error(8L, excelImport, 2, "ten_san_pham");
        PageRequest pageable = PageRequest.of(0, 20);

        when(excelImportRepository.findById(99L)).thenReturn(Optional.of(excelImport));
        when(excelImportErrorRepository.findByExcelImportId(99L, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));

        PageResponse<ExcelImportErrorResponse> response = validationService.listErrors(99L, pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content()).extracting(ExcelImportErrorResponse::id).containsExactly(7L, 8L);
        assertThat(response.content()).extracting(ExcelImportErrorResponse::importId).containsExactly(99L, 99L);
        assertThat(response.content()).extracting(ExcelImportErrorResponse::rowNumber).containsExactly(2, 2);
        assertThat(response.content()).extracting(ExcelImportErrorResponse::columnName)
                .containsExactly("ma_san_pham", "ten_san_pham");
        assertThat(response.content()).extracting(ExcelImportErrorResponse::originalValue)
                .containsExactly("raw-ma_san_pham", "raw-ten_san_pham");
        assertThat(response.content()).extracting(ExcelImportErrorResponse::message)
                .containsExactly("message-ma_san_pham", "message-ten_san_pham");
        assertThat(response.content()).extracting(ExcelImportErrorResponse::suggestion)
                .containsExactly("suggestion-ma_san_pham", "suggestion-ten_san_pham");
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(excelImport.getStatus()).isEqualTo(ExcelImportStatus.CO_LOI);
        assertThat(excelImport.getTotalRows()).isEqualTo(3);
        assertThat(excelImport.getValidRows()).isEqualTo(1);
        assertThat(excelImport.getErrorRows()).isEqualTo(2);

        verify(excelImportRepository, never()).save(any(ExcelImport.class));
        verify(excelImportErrorRepository, never()).deleteByExcelImportId(anyLong());
        verify(excelImportErrorRepository, never()).saveAll(any());
        verifyNoInteractions(categoryRepository, productRepository, warehouseRepository);
    }

    @Test
    void listErrors_existingImportWithoutErrorsReturnsEmptyPage() {
        ExcelImport excelImport = new ExcelImport();
        excelImport.setId(100L);
        PageRequest pageable = PageRequest.of(0, 20);

        when(excelImportRepository.findById(100L)).thenReturn(Optional.of(excelImport));
        when(excelImportErrorRepository.findByExcelImportId(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ExcelImportErrorResponse> response = validationService.listErrors(100L, pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(excelImportRepository, never()).save(any(ExcelImport.class));
        verify(excelImportErrorRepository, never()).deleteByExcelImportId(anyLong());
        verify(excelImportErrorRepository, never()).saveAll(any());
    }

    @Test
    void listErrors_missingImportShouldReturnNotFound() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(excelImportRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.listErrors(404L, pageable))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Lan import khong ton tai.");

        verify(excelImportErrorRepository, never()).findByExcelImportId(anyLong(), any());
        verify(excelImportRepository, never()).save(any(ExcelImport.class));
        verify(excelImportErrorRepository, never()).deleteByExcelImportId(anyLong());
        verify(excelImportErrorRepository, never()).saveAll(any());
    }

    private MockMultipartFile xlsxFile(XSSFWorkbook workbook) throws IOException {
        try (workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new MockMultipartFile("file", "sample.xlsx", XLSX, out.toByteArray());
        }
    }

    private XSSFWorkbook workbook(List<String> productHeaders, List<List<String>> productRows, List<List<String>> openingRows, List<List<String>> extraRows) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        if (productHeaders != null) {
            createSheet(workbook, ExcelImportSheetName.SAN_PHAM.getSheetName(), productHeaders, productRows);
        }
        if (openingRows != null) {
            createSheet(workbook, ExcelImportSheetName.TON_DAU_KY.getSheetName(), ExcelImportTemplateConstants.OPENING_STOCK_HEADERS, openingRows);
        }
        if (extraRows != null) {
            createSheet(workbook, ExcelImportSheetName.DANH_MUC_THAM_CHIEU.getSheetName(), List.of("x"), extraRows);
        }
        return workbook;
    }

    private void createSheet(XSSFWorkbook workbook, String sheetName, List<String> headers, List<List<String>> rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) {
            headerRow.createCell(index).setCellValue(headers.get(index));
        }
        if (rows == null) {
            return;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<String> values = rows.get(rowIndex);
            for (int cellIndex = 0; cellIndex < values.size(); cellIndex++) {
                String value = values.get(cellIndex);
                if (value != null) {
                    row.createCell(cellIndex).setCellValue(value);
                }
            }
        }
    }

    private ExcelImportError error(Long id, ExcelImport excelImport, Integer rowNumber, String columnName) {
        ExcelImportError error = new ExcelImportError();
        error.setId(id);
        error.setExcelImport(excelImport);
        error.setRowNumber(rowNumber);
        error.setColumnName(columnName);
        error.setOriginalValue("raw-" + columnName);
        error.setMessage("message-" + columnName);
        error.setSuggestion("suggestion-" + columnName);
        return error;
    }
}
