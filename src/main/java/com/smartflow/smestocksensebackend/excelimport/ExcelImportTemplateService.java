package com.smartflow.smestocksensebackend.excelimport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelImportTemplateService {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            createSheet(workbook, ExcelImportSheetName.SAN_PHAM.getSheetName(), headerStyle,
                    ExcelImportTemplateConstants.PRODUCT_HEADERS);
            createSheet(workbook, ExcelImportSheetName.TON_DAU_KY.getSheetName(), headerStyle,
                    ExcelImportTemplateConstants.OPENING_STOCK_HEADERS);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Khong the tao file Excel mau.", exception);
        }
    }

    private void createSheet(Workbook workbook, String sheetName, CellStyle headerStyle, List<String> headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row row = sheet.createRow(0);
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(index);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }
}
