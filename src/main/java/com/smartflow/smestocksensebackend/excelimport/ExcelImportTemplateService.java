package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelImportTemplateService {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            createInstructionSheet(workbook);
            createProductSheet(workbook, headerStyle);
            createOpeningStockSheet(workbook, headerStyle);
            createCategoryReferenceSheet(workbook, headerStyle);
            createWarehouseReferenceSheet(workbook, headerStyle);
            createValidValuesSheet(workbook, headerStyle);
            createValidationRulesSheet(workbook);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file Excel mẫu.", exception);
        }
    }

    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.HUONG_DAN.getSheetName());
        List<String> instructions = List.of(
                "1. Không đổi tên sheet.",
                "2. Không đổi tên cột ở dòng tiêu đề.",
                "3. Không xóa dòng tiêu đề.",
                "4. Chỉ nhập dữ liệu từ dòng thứ 2 trở xuống.",
                "5. Không nhập ID hệ thống.",
                "6. Chỉ dùng mã nghiệp vụ như ma_san_pham, ma_danh_muc, ma_kho.",
                "7. Các cột bắt buộc không được bỏ trống.",
                "8. ma_san_pham phải duy nhất trong sheet 01_SanPham.",
                "9. Cặp ma_kho + ma_san_pham phải duy nhất trong sheet 02_TonDauKy.",
                "10. gia_ban, ton_toi_thieu, ton_toi_da, so_luong_ton phải là số không âm.",
                "11. Nếu nhập tồn đầu kỳ cho sản phẩm mới, sản phẩm đó phải có trong sheet 01_SanPham hoặc đã tồn tại trong hệ thống.",
                "12. Hệ thống sẽ kiểm tra file trước. Nếu có lỗi, hệ thống trả danh sách lỗi theo dòng/cột để người dùng sửa lại."
        );
        writeSingleColumn(sheet, instructions);
    }

    private void createProductSheet(Workbook workbook, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.SAN_PHAM.getSheetName());
        writeHeader(sheet, headerStyle, ExcelImportTemplateConstants.PRODUCT_HEADERS);
        writeRow(sheet, 1, List.of("SP_MAU_001", "Sản phẩm mẫu", "SKU-MAU-001", "893000000001", "cái",
                "DM_MAU", "100000", "0", "100", "HOAT_DONG"));
        autoSize(sheet, ExcelImportTemplateConstants.PRODUCT_HEADERS.size());
    }

    private void createOpeningStockSheet(Workbook workbook, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.TON_DAU_KY.getSheetName());
        writeHeader(sheet, headerStyle, ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);
        writeRow(sheet, 1, List.of("KHO_MAU", "SP_MAU_001", "10"));
        autoSize(sheet, ExcelImportTemplateConstants.OPENING_STOCK_HEADERS.size());
    }

    private void createCategoryReferenceSheet(Workbook workbook, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.DANH_MUC_THAM_CHIEU.getSheetName());
        List<String> headers = List.of("ma_danh_muc", "ten_danh_muc", "trang_thai");
        writeHeader(sheet, headerStyle, headers);

        List<Category> categories = categoryRepository.findAll(Sort.by("code"));
        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            writeRow(sheet, index + 1, List.of(
                    nullToBlank(category.getCode()),
                    nullToBlank(category.getName()),
                    category.getStatus() == null ? "" : category.getStatus().name()
            ));
        }
        autoSize(sheet, headers.size());
    }

    private void createWarehouseReferenceSheet(Workbook workbook, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.KHO_THAM_CHIEU.getSheetName());
        List<String> headers = List.of("ma_kho", "ten_kho", "trang_thai");
        writeHeader(sheet, headerStyle, headers);

        List<Warehouse> warehouses = warehouseRepository.findAll(Sort.by("code"));
        for (int index = 0; index < warehouses.size(); index++) {
            Warehouse warehouse = warehouses.get(index);
            writeRow(sheet, index + 1, List.of(
                    nullToBlank(warehouse.getCode()),
                    nullToBlank(warehouse.getName()),
                    warehouse.getStatus() == null ? "" : warehouse.getStatus().name()
            ));
        }
        autoSize(sheet, headers.size());
    }

    private void createValidValuesSheet(Workbook workbook, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.GIA_TRI_HOP_LE.getSheetName());
        writeHeader(sheet, headerStyle, List.of("nhom", "gia_tri"));
        writeRow(sheet, 1, List.of("Product status", "HOAT_DONG"));
        writeRow(sheet, 2, List.of("Product status", "NGUNG_HOAT_DONG"));
        writeRow(sheet, 3, List.of("Import modes", ExcelImportMode.PRODUCT_ONLY.name()));
        writeRow(sheet, 4, List.of("Import modes", ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name()));
        autoSize(sheet, 2);
    }

    private void createValidationRulesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(ExcelImportSheetName.QUY_TAC_KIEM_TRA.getSheetName());
        writeSingleColumn(sheet, List.of(
                "Product rules",
                "- Thiếu sheet 01_SanPham -> lỗi đối với PRODUCT_ONLY.",
                "- Sai header -> lỗi.",
                "- ma_san_pham bắt buộc.",
                "- ma_san_pham không được trùng trong file.",
                "- ten_san_pham bắt buộc.",
                "- don_vi_tinh bắt buộc.",
                "- ma_danh_muc bắt buộc và phải tồn tại trong danh_muc.ma_danh_muc.",
                "- sku phải duy nhất nếu có nhập.",
                "- ma_vach phải duy nhất nếu có nhập.",
                "- gia_ban phải >= 0 nếu có nhập.",
                "- ton_toi_thieu phải >= 0 nếu có nhập.",
                "- ton_toi_da phải >= 0 nếu có nhập.",
                "- ton_toi_thieu <= ton_toi_da nếu cả hai có nhập.",
                "- trang_thai chỉ được là HOAT_DONG, NGUNG_HOAT_DONG hoặc bỏ trống.",
                "- Nếu bỏ trống trang_thai thì mặc định HOAT_DONG.",
                "Opening stock rules",
                "- Sheet 02_TonDauKy chỉ bắt buộc với PRODUCT_WITH_OPENING_STOCK.",
                "- Sai header -> lỗi.",
                "- ma_kho bắt buộc và phải tồn tại trong kho.ma_kho.",
                "- ma_san_pham bắt buộc và phải tồn tại trong DB hoặc trong sheet 01_SanPham.",
                "- so_luong_ton bắt buộc và phải >= 0.",
                "- Cặp ma_kho + ma_san_pham không được trùng trong file."
        ));
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle, List<String> headers) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeSingleColumn(Sheet sheet, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            sheet.createRow(index).createCell(0).setCellValue(values.get(index));
        }
        sheet.autoSizeColumn(0);
    }

    private void writeRow(Sheet sheet, int rowIndex, List<String> values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
