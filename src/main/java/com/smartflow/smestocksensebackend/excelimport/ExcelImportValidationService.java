package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationErrorResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportError;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportErrorRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExcelImportValidationService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream"
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ExcelImportRepository excelImportRepository;
    private final ExcelImportErrorRepository excelImportErrorRepository;

    public ExcelImportValidationResponse validate(MultipartFile file, String loaiImport, Long khoId) {
        validateRequest(file, loaiImport);
        ExcelImportMode mode = parseMode(loaiImport);

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            ValidationState state = new ValidationState(mode.name());
            validateWarehouseIfProvided(khoId, state);
            validateWorkbook(workbook, mode, state);
            return state.toResponse();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("File Excel không hợp lệ.");
        }
    }

    @Transactional
    public ExcelImportValidationResponse validateAndPersistErrors(Long importId, MultipartFile file, String loaiImport, Long khoId) {
        ExcelImport excelImport = excelImportRepository.findById(importId)
                .orElseThrow(() -> new NotFoundException("Lan import khong ton tai."));
        ExcelImportValidationResponse response = validate(file, loaiImport, khoId);

        excelImportErrorRepository.deleteByExcelImportId(importId);
        excelImport.setTotalRows(response.tongSoDong());
        excelImport.setValidRows(response.soDongHopLe());
        excelImport.setErrorRows(response.soDongLoi());
        excelImport.setStatus(response.valid() ? ExcelImportStatus.SAN_SANG_IMPORT : ExcelImportStatus.CO_LOI);
        excelImportRepository.save(excelImport);
        persistValidationErrors(excelImport, response.errors());

        return response;
    }

    private void persistValidationErrors(ExcelImport excelImport, List<ExcelImportValidationErrorResponse> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        List<ExcelImportError> importErrors = errors.stream()
                .map(error -> toImportError(excelImport, error))
                .toList();
        excelImportErrorRepository.saveAll(importErrors);
    }

    private ExcelImportError toImportError(ExcelImport excelImport, ExcelImportValidationErrorResponse error) {
        ExcelImportError importError = new ExcelImportError();
        importError.setExcelImport(excelImport);
        importError.setRowNumber(error.rowNumber());
        importError.setColumnName(error.columnName());
        importError.setOriginalValue(error.rawValue());
        importError.setMessage(error.message());
        importError.setSuggestion(error.suggestion());
        return importError;
    }

    private void validateRequest(MultipartFile file, String loaiImport) {
        if (file == null) {
            throw new BadRequestException("file is required.");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("file must not be empty.");
        }
        String fileName = safeOriginalFileName(file);
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BadRequestException("file must be .xlsx.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("file content type is not supported.");
        }

        if (loaiImport == null || loaiImport.isBlank()) {
            throw new BadRequestException("loaiImport is required.");
        }
        parseMode(loaiImport);
    }

    private ExcelImportMode parseMode(String loaiImport) {
        try {
            return ExcelImportMode.valueOf(loaiImport.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("loaiImport is not supported.");
        }
    }

    private void validateWarehouseIfProvided(Long khoId, ValidationState state) {
        if (khoId == null) {
            return;
        }
        if (!warehouseRepository.existsById(khoId)) {
            state.addError(new ExcelImportValidationErrorResponse(
                    null,
                    null,
                    "khoId",
                    String.valueOf(khoId),
                    "Kho hàng không tồn tại.",
                    "Chọn lại kho hợp lệ."
            ));
        }
    }

    private void validateWorkbook(Workbook workbook, ExcelImportMode mode, ValidationState state) {
        Sheet productSheet = workbook.getSheet(ExcelImportSheetName.SAN_PHAM.getSheetName());
        Sheet openingSheet = workbook.getSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName());
        Set<String> productCodesInWorkbook = new LinkedHashSet<>();

        if (mode == ExcelImportMode.PRODUCT_ONLY) {
            validateProductSheetRequired(productSheet, state, productCodesInWorkbook);
            return;
        }

        if (productSheet != null && hasAnyBusinessRow(productSheet)) {
            validateProductSheet(productSheet, state, productCodesInWorkbook);
        } else if (productSheet != null) {
            validateProductHeader(productSheet, state);
        }

        validateOpeningStockSheetRequired(openingSheet, state, productCodesInWorkbook);
    }

    private void validateProductSheetRequired(Sheet productSheet, ValidationState state, Set<String> productCodesInWorkbook) {
        if (productSheet == null) {
            state.addError(new ExcelImportValidationErrorResponse(
                    ExcelImportSheetName.SAN_PHAM.getSheetName(),
                    1,
                    "header",
                    null,
                    "Thiếu sheet 01_SanPham.",
                    "Thêm sheet 01_SanPham theo đúng template."
            ));
            return;
        }
        validateProductSheet(productSheet, state, productCodesInWorkbook);
    }

    private void validateOpeningStockSheetRequired(Sheet openingSheet, ValidationState state, Set<String> productCodesInWorkbook) {
        if (openingSheet == null) {
            state.addError(new ExcelImportValidationErrorResponse(
                    ExcelImportSheetName.TON_DAU_KY.getSheetName(),
                    1,
                    "header",
                    null,
                    "Thiếu sheet 02_TonDauKy.",
                    "Thêm sheet 02_TonDauKy theo đúng template."
            ));
            return;
        }
        validateOpeningStockSheet(openingSheet, state, productCodesInWorkbook);
    }

    private void validateProductSheet(Sheet sheet, ValidationState state, Set<String> productCodesInWorkbook) {
        if (!validateProductHeader(sheet, state)) {
            return;
        }

        Set<String> seenCodes = new LinkedHashSet<>();
        Set<String> seenSkus = new LinkedHashSet<>();
        Set<String> seenBarcodes = new LinkedHashSet<>();
        String sheetName = ExcelImportSheetName.SAN_PHAM.getSheetName();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, ExcelImportTemplateConstants.PRODUCT_HEADERS.size())) {
                continue;
            }

            state.totalRows++;
            String code = readCell(row, 0);
            String name = readCell(row, 1);
            String sku = readCell(row, 2);
            String barcode = readCell(row, 3);
            String unit = readCell(row, 4);
            String categoryCode = readCell(row, 5);
            String priceRaw = readCell(row, 6);
            String minStockRaw = readCell(row, 7);
            String maxStockRaw = readCell(row, 8);
            String statusRaw = readCell(row, 9);

            if (isBlank(code)) {
                state.addRowError(sheetName, rowIndex + 1, "ma_san_pham", code, "Mã sản phẩm không được để trống.", "Nhập mã sản phẩm.");
            } else {
                productCodesInWorkbook.add(normalize(code));
                if (!seenCodes.add(normalize(code))) {
                    state.addRowError(sheetName, rowIndex + 1, "ma_san_pham", code, "Mã sản phẩm bị trùng trong file.", "Mỗi mã sản phẩm chỉ được xuất hiện một lần.");
                }
            }

            if (isBlank(name)) {
                state.addRowError(sheetName, rowIndex + 1, "ten_san_pham", name, "Tên sản phẩm không được để trống.", "Nhập tên sản phẩm.");
            }
            if (isBlank(unit)) {
                state.addRowError(sheetName, rowIndex + 1, "don_vi_tinh", unit, "Đơn vị tính không được để trống.", "Nhập đơn vị tính.");
            }

            if (isBlank(categoryCode)) {
                state.addRowError(sheetName, rowIndex + 1, "ma_danh_muc", categoryCode, "Mã danh mục không được để trống.", "Chọn danh mục hợp lệ.");
            } else if (!categoryRepository.existsByNormalizedCode(categoryCode)) {
                state.addRowError(sheetName, rowIndex + 1, "ma_danh_muc", categoryCode, "Danh mục không tồn tại.", "Kiểm tra lại mã danh mục.");
            }

            validateNonNegativeDecimal(sheetName, rowIndex + 1, "gia_ban", priceRaw, state);
            validateNonNegativeInteger(sheetName, rowIndex + 1, "ton_toi_thieu", minStockRaw, state);
            validateNonNegativeInteger(sheetName, rowIndex + 1, "ton_toi_da", maxStockRaw, state);
            validateStockRange(sheetName, rowIndex + 1, minStockRaw, maxStockRaw, state);
            validateStatus(sheetName, rowIndex + 1, statusRaw, state);

            if (!isBlank(sku)) {
                if (!seenSkus.add(normalize(sku))) {
                    state.addRowError(sheetName, rowIndex + 1, "sku", sku, "SKU bị trùng trong file.", "Dùng SKU khác cho từng sản phẩm.");
                } else if (productRepository.existsBySkuIgnoreCase(sku)) {
                    state.addRowError(sheetName, rowIndex + 1, "sku", sku, "SKU đã tồn tại trong hệ thống.", "Đổi SKU trước khi import.");
                }
            }

            if (!isBlank(barcode)) {
                if (!seenBarcodes.add(normalize(barcode))) {
                    state.addRowError(sheetName, rowIndex + 1, "ma_vach", barcode, "Mã vạch bị trùng trong file.", "Dùng mã vạch khác cho từng sản phẩm.");
                } else if (productRepository.existsByBarcodeIgnoreCase(barcode)) {
                    state.addRowError(sheetName, rowIndex + 1, "ma_vach", barcode, "Mã vạch đã tồn tại trong hệ thống.", "Đổi mã vạch trước khi import.");
                }
            }
        }
    }

    private void validateOpeningStockSheet(Sheet sheet, ValidationState state, Set<String> productCodesInWorkbook) {
        if (!validateOpeningStockHeader(sheet, state)) {
            return;
        }

        Set<String> seenPairs = new LinkedHashSet<>();
        String sheetName = ExcelImportSheetName.TON_DAU_KY.getSheetName();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, ExcelImportTemplateConstants.OPENING_STOCK_HEADERS.size())) {
                continue;
            }

            state.totalRows++;
            String warehouseCode = readCell(row, 0);
            String productCode = readCell(row, 1);
            String quantityRaw = readCell(row, 2);

            if (isBlank(warehouseCode)) {
                state.addRowError(sheetName, rowIndex + 1, "ma_kho", warehouseCode, "Mã kho không được để trống.", "Nhập mã kho.");
            } else if (!warehouseRepository.existsByCodeIgnoreCase(warehouseCode)) {
                state.addRowError(sheetName, rowIndex + 1, "ma_kho", warehouseCode, "Kho hàng không tồn tại.", "Kiểm tra lại mã kho.");
            }

            if (isBlank(productCode)) {
                state.addRowError(sheetName, rowIndex + 1, "ma_san_pham", productCode, "Mã sản phẩm không được để trống.", "Nhập mã sản phẩm.");
            } else if (!productRepository.existsByCodeIgnoreCase(productCode) && !productCodesInWorkbook.contains(normalize(productCode))) {
                state.addRowError(sheetName, rowIndex + 1, "ma_san_pham", productCode, "Sản phẩm không tồn tại.", "Thêm sản phẩm vào sheet 01_SanPham hoặc dùng mã đang có trong hệ thống.");
            }

            if (isBlank(quantityRaw)) {
                state.addRowError(sheetName, rowIndex + 1, "so_luong_ton", quantityRaw, "Số lượng tồn không được để trống.", "Nhập số lượng tồn.");
            } else {
                validateNonNegativeDecimal(sheetName, rowIndex + 1, "so_luong_ton", quantityRaw, state);
            }

            if (!isBlank(warehouseCode) && !isBlank(productCode)) {
                String pairKey = normalize(warehouseCode) + "|" + normalize(productCode);
                if (!seenPairs.add(pairKey)) {
                    state.addRowError(sheetName, rowIndex + 1, "ma_kho", warehouseCode, "Cặp mã kho và mã sản phẩm bị trùng trong file.", "Mỗi cặp kho - sản phẩm chỉ được xuất hiện một lần.");
                }
            }
        }
    }

    private boolean validateProductHeader(Sheet sheet, ValidationState state) {
        boolean valid = headerMatches(sheet, ExcelImportTemplateConstants.PRODUCT_HEADERS);
        if (!valid) {
            state.addError(new ExcelImportValidationErrorResponse(
                    ExcelImportSheetName.SAN_PHAM.getSheetName(),
                    1,
                    "header",
                    headerRowValue(sheet),
                    "Header sheet 01_SanPham không khớp template.",
                    "Giữ đúng thứ tự cột của template."
            ));
        }
        return valid;
    }

    private boolean validateOpeningStockHeader(Sheet sheet, ValidationState state) {
        boolean valid = headerMatches(sheet, ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);
        if (!valid) {
            state.addError(new ExcelImportValidationErrorResponse(
                    ExcelImportSheetName.TON_DAU_KY.getSheetName(),
                    1,
                    "header",
                    headerRowValue(sheet),
                    "Header sheet 02_TonDauKy không khớp template.",
                    "Giữ đúng thứ tự cột của template."
            ));
        }
        return valid;
    }

    private void validateStatus(String sheetName, int rowNumber, String value, ValidationState state) {
        if (isBlank(value)) {
            return;
        }
        try {
            com.smartflow.smestocksensebackend.entity.ProductStatus.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            state.addRowError(sheetName, rowNumber, "trang_thai", value, "Trạng thái sản phẩm không hợp lệ.", "Chỉ dùng HOAT_DONG hoặc NGUNG_HOAT_DONG.");
        }
    }

    private void validateNonNegativeDecimal(String sheetName, int rowNumber, String columnName, String rawValue, ValidationState state) {
        if (isBlank(rawValue)) {
            return;
        }
        BigDecimal value = parseDecimal(rawValue);
        if (value == null) {
            state.addRowError(sheetName, rowNumber, columnName, rawValue, "Giá trị số không hợp lệ.", "Nhập số hợp lệ.");
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            state.addRowError(sheetName, rowNumber, columnName, rawValue, "Giá trị không được âm.", "Nhập giá trị lớn hơn hoặc bằng 0.");
        }
    }

    private void validateNonNegativeInteger(String sheetName, int rowNumber, String columnName, String rawValue, ValidationState state) {
        if (isBlank(rawValue)) {
            return;
        }
        BigDecimal value = parseDecimal(rawValue);
        if (value == null || value.stripTrailingZeros().scale() > 0) {
            state.addRowError(sheetName, rowNumber, columnName, rawValue, "Giá trị số không hợp lệ.", "Nhập số nguyên hợp lệ.");
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            state.addRowError(sheetName, rowNumber, columnName, rawValue, "Giá trị không được âm.", "Nhập giá trị lớn hơn hoặc bằng 0.");
        }
    }

    private void validateStockRange(String sheetName, int rowNumber, String minRaw, String maxRaw, ValidationState state) {
        if (isBlank(minRaw) || isBlank(maxRaw)) {
            return;
        }
        BigDecimal min = parseDecimal(minRaw);
        BigDecimal max = parseDecimal(maxRaw);
        if (min == null || max == null) {
            return;
        }
        if (min.compareTo(max) > 0) {
            state.addRowError(sheetName, rowNumber, "ton_toi_thieu", minRaw, "Tồn tối thiểu phải nhỏ hơn hoặc bằng tồn tối đa.", "Điều chỉnh lại khoảng tồn kho.");
        }
    }

    private boolean hasAnyBusinessRow(Sheet sheet) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            if (!isBlankRow(sheet.getRow(rowIndex), sheet.getRow(0) == null ? 0 : sheet.getRow(0).getLastCellNum())) {
                return true;
            }
        }
        return false;
    }

    private boolean headerMatches(Sheet sheet, List<String> expectedHeaders) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() != expectedHeaders.size()) {
            return false;
        }
        for (int index = 0; index < expectedHeaders.size(); index++) {
            if (!expectedHeaders.get(index).equals(readCell(headerRow, index))) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlankRow(Row row, int columnCount) {
        if (row == null) {
            return true;
        }
        int lastCell = Math.max(row.getLastCellNum(), (short) columnCount);
        for (int index = 0; index < lastCell; index++) {
            if (!isBlank(readCell(row, index))) {
                return false;
            }
        }
        return true;
    }

    private String headerRowValue(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < headerRow.getLastCellNum(); index++) {
            values.add(readCell(headerRow, index));
        }
        return String.join(" | ", values);
    }

    private String readCell(Row row, int index) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        return new DataFormatter().formatCellValue(cell).trim();
    }

    private BigDecimal parseDecimal(String rawValue) {
        try {
            return new BigDecimal(rawValue.replace(",", "").trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private String safeOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BadRequestException("file name is required.");
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank()) {
            throw new BadRequestException("file name is required.");
        }
        return fileName;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class ValidationState {
        private final String loaiImport;
        private final List<ExcelImportValidationErrorResponse> errors = new ArrayList<>();
        private final Set<String> errorRows = new LinkedHashSet<>();
        private int totalRows;

        private ValidationState(String loaiImport) {
            this.loaiImport = loaiImport;
        }

        private void addError(ExcelImportValidationErrorResponse error) {
            errors.add(error);
            if (error.rowNumber() != null) {
                errorRows.add(error.sheetName() + "#" + error.rowNumber());
            }
        }

        private void addRowError(String sheetName, int rowNumber, String columnName, String rawValue, String message, String suggestion) {
            addError(new ExcelImportValidationErrorResponse(sheetName, rowNumber, columnName, rawValue, message, suggestion));
        }

        private ExcelImportValidationResponse toResponse() {
            return new ExcelImportValidationResponse(
                    errors.isEmpty(),
                    loaiImport,
                    totalRows,
                    Math.max(totalRows - errorRows.size(), 0),
                    errorRows.size(),
                    List.copyOf(errors)
            );
        }
    }
}
