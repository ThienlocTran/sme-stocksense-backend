package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportApplyResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportErrorRepository;
import com.smartflow.smestocksensebackend.repository.ExcelImportRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
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
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExcelImportApplyService {

    private static final String DECIMAL_PATTERN = "-?\\d+(\\.\\d+)?";

    private final ExcelImportRepository excelImportRepository;
    private final ExcelImportErrorRepository excelImportErrorRepository;
    private final ExcelImportValidationService excelImportValidationService;
    private final ExcelImportChecksumService excelImportChecksumService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional
    public ExcelImportApplyResponse apply(Long importId, MultipartFile file) {
        ExcelImport excelImport = excelImportRepository.findByIdForUpdate(importId)
                .orElseThrow(() -> new NotFoundException("Lan import khong ton tai."));

        validateApplyPreconditions(excelImport, file);
        ExcelImportMode mode = parseMode(excelImport.getImportType());
        if (mode == ExcelImportMode.PRODUCT_WITH_OPENING_STOCK && excelImport.getCreatedBy() == null) {
            throw new BadRequestException("Lan import khong co nguoi tao de ghi nhan giao dich kho.");
        }
        Long warehouseId = excelImport.getWarehouse() == null ? null : excelImport.getWarehouse().getId();
        ExcelImportValidationResponse validation = excelImportValidationService.validate(file, mode.name(), warehouseId);
        if (!validation.valid()) {
            throw new BadRequestException("File Excel khong con hop le de import.");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            applyProductsIfNeeded(workbook, mode);
            if (mode == ExcelImportMode.PRODUCT_WITH_OPENING_STOCK) {
                applyOpeningStock(workbook, excelImport);
            }
        } catch (BadRequestException | NotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("File Excel khong hop le.", exception);
        }

        excelImport.setStatus(ExcelImportStatus.DA_IMPORT);
        excelImport.setCompletedAt(LocalDateTime.now());
        return ExcelImportApplyResponse.from(excelImportRepository.save(excelImport));
    }

    private void validateApplyPreconditions(ExcelImport excelImport, MultipartFile file) {
        if (excelImport.getStatus() != ExcelImportStatus.DA_XAC_NHAN) {
            throw new BadRequestException("Lan import chua duoc xac nhan hoac da duoc import.");
        }
        if (isBlank(excelImport.getChecksumFileSha256())) {
            throw new BadRequestException("Lan import chua co checksum file da validate.");
        }
        String checksum = excelImportChecksumService.sha256(file);
        if (!excelImport.getChecksumFileSha256().equalsIgnoreCase(checksum)) {
            throw new BadRequestException("File Excel khong khop voi file da validate.");
        }
        if (excelImport.getTotalRows() == null || excelImport.getTotalRows() <= 0
                || excelImport.getValidRows() == null || excelImport.getValidRows() <= 0
                || excelImport.getErrorRows() == null || excelImport.getErrorRows() > 0) {
            throw new BadRequestException("Lan import chua co du lieu hop le de import.");
        }
        if (excelImportErrorRepository.existsByExcelImportId(excelImport.getId())) {
            throw new BadRequestException("Lan import con loi da luu, khong the import.");
        }
    }

    private ExcelImportMode parseMode(String importType) {
        try {
            return ExcelImportMode.valueOf(importType);
        } catch (Exception exception) {
            throw new BadRequestException("loaiImport is not supported.", exception);
        }
    }

    private void applyProductsIfNeeded(Workbook workbook, ExcelImportMode mode) {
        Sheet sheet = workbook.getSheet(ExcelImportSheetName.SAN_PHAM.getSheetName());
        if (sheet == null) {
            if (mode == ExcelImportMode.PRODUCT_ONLY) {
                throw new BadRequestException("Thieu sheet 01_SanPham.");
            }
            return;
        }
        if (!hasAnyBusinessRow(sheet, ExcelImportTemplateConstants.PRODUCT_HEADERS.size())) {
            return;
        }

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, ExcelImportTemplateConstants.PRODUCT_HEADERS.size())) {
                continue;
            }
            upsertProduct(row);
        }
    }

    private void upsertProduct(Row row) {
        String code = requiredCell(row, 0, "ma_san_pham").trim().toUpperCase(Locale.ROOT);
        String categoryCode = requiredCell(row, 5, "ma_danh_muc");
        Category category = categoryRepository.findByNormalizedCode(categoryCode)
                .orElseThrow(() -> new BadRequestException("Danh muc khong ton tai."));

        Product product = productRepository.findByCode(code).orElseGet(Product::new);
        product.setCode(code);
        product.setName(requiredCell(row, 1, "ten_san_pham").trim());
        product.setSku(blankToNull(readCell(row, 2)));
        product.setBarcode(blankToNull(readCell(row, 3)));
        product.setUnit(requiredCell(row, 4, "don_vi_tinh").trim());
        product.setCategory(category);
        product.setPrice(parseOptionalDecimal(readCell(row, 6)));
        Integer minStock = parseOptionalInteger(readCell(row, 7));
        product.setMinStock(minStock == null ? 0 : minStock);
        product.setMaxStock(parseOptionalInteger(readCell(row, 8)));
        product.setStatus(parseStatusOrDefault(readCell(row, 9)));

        productRepository.saveAndFlush(product);
    }

    private void applyOpeningStock(Workbook workbook, ExcelImport excelImport) {
        Sheet sheet = workbook.getSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName());
        if (sheet == null) {
            throw new BadRequestException("Thieu sheet 02_TonDauKy.");
        }

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlankRow(row, ExcelImportTemplateConstants.OPENING_STOCK_HEADERS.size())) {
                continue;
            }
            setOpeningStock(row, excelImport);
        }
    }

    private void setOpeningStock(Row row, ExcelImport excelImport) {
        Warehouse warehouse = warehouseRepository.findByCodeIgnoreCase(requiredCell(row, 0, "ma_kho"))
                .orElseThrow(() -> new BadRequestException("Kho hang khong ton tai."));
        Product product = productRepository.findByCode(requiredCell(row, 1, "ma_san_pham").trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("San pham khong ton tai."));
        Integer quantity = parseRequiredInteger(readCell(row, 2), "so_luong_ton");

        InventoryLevel inventoryLevel = inventoryLevelRepository
                .findByProductIdAndWarehouseIdForUpdate(product.getId(), warehouse.getId())
                .orElseGet(() -> {
                    InventoryLevel created = new InventoryLevel();
                    created.setProduct(product);
                    created.setWarehouse(warehouse);
                    return created;
                });
        Integer quantityBefore = inventoryLevel.getQuantity() == null ? 0 : inventoryLevel.getQuantity();
        inventoryLevel.setQuantity(quantity);
        inventoryLevelRepository.saveAndFlush(inventoryLevel);
        if (!quantityBefore.equals(quantity)) {
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProduct(product);
            transaction.setWarehouse(warehouse);
            transaction.setTransactionType(InventoryTransactionType.NHAP_DAU_KY);
            transaction.setQuantity(Math.abs(quantity - quantityBefore));
            transaction.setQuantityBefore(quantityBefore);
            transaction.setQuantityAfter(quantity);
            transaction.setImportBatchId(excelImport.getId());
            transaction.setCreatedBy(excelImport.getCreatedBy());
            transaction.setNote("Nhap dau ky tu import Excel.");
            inventoryTransactionRepository.saveAndFlush(transaction);
        }
    }

    private ProductStatus parseStatusOrDefault(String value) {
        if (isBlank(value)) {
            return ProductStatus.HOAT_DONG;
        }
        try {
            return ProductStatus.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Trang thai san pham khong hop le.", exception);
        }
    }

    private BigDecimal parseOptionalDecimal(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.matches(DECIMAL_PATTERN)) {
            throw new BadRequestException("Gia tri so khong hop le.");
        }
        try {
            return new BigDecimal(normalized);
        } catch (Exception exception) {
            throw new BadRequestException("Gia tri so khong hop le.", exception);
        }
    }

    private Integer parseOptionalInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        return parseRequiredInteger(value, "number");
    }

    private Integer parseRequiredInteger(String value, String columnName) {
        BigDecimal decimal = parseOptionalDecimal(value);
        if (decimal == null || decimal.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException(columnName + " phai la so nguyen.");
        }
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException exception) {
            throw new BadRequestException(columnName + " vuot gioi han cho phep.", exception);
        }
    }

    private String requiredCell(Row row, int index, String columnName) {
        String value = readCell(row, index);
        if (isBlank(value)) {
            throw new BadRequestException(columnName + " khong duoc de trong.");
        }
        return value;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean hasAnyBusinessRow(Sheet sheet, int columnCount) {
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            if (!isBlankRow(sheet.getRow(rowIndex), columnCount)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlankRow(Row row, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < columnCount; index++) {
            if (!isBlank(readCell(row, index))) {
                return false;
            }
        }
        return true;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
