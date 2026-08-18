package com.smartflow.smestocksensebackend.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailItemResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.StockDocumentExportService;
import com.smartflow.smestocksensebackend.service.document.GeneratedDocument;
import com.smartflow.smestocksensebackend.util.VietnameseNumberToWords;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockDocumentExportServiceImpl implements StockDocumentExportService {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final ImportReceiptService importReceiptService;
    private final ExportReceiptService exportReceiptService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartnerRepository partnerRepository;

    @Override
    public GeneratedDocument exportImportReceiptPdf(Long receiptId) {
        ReceiptDocument document = buildImportDocument(importReceiptService.getDetail(receiptId));
        return renderPdf(document, "Mẫu số 01 - VT", "PHIẾU NHẬP KHO", "Thực nhập",
                fileName("phieu-nhap-", document.code(), "pdf"));
    }

    @Override
    public GeneratedDocument exportImportReceiptExcel(Long receiptId) {
        ReceiptDocument document = buildImportDocument(importReceiptService.getDetail(receiptId));
        return renderExcel(document, "Mẫu số 01 - VT", "PHIẾU NHẬP KHO", "Thực nhập",
                fileName("phieu-nhap-", document.code(), "xlsx"));
    }

    @Override
    public GeneratedDocument exportExportReceiptPdf(Long receiptId) {
        ReceiptDocument document = buildExportDocument(exportReceiptService.getDetail(receiptId));
        return renderPdf(document, "Mẫu số 02 - VT", "PHIẾU XUẤT KHO", "Thực xuất",
                fileName("phieu-xuat-", document.code(), "pdf"));
    }

    @Override
    public GeneratedDocument exportExportReceiptExcel(Long receiptId) {
        ReceiptDocument document = buildExportDocument(exportReceiptService.getDetail(receiptId));
        return renderExcel(document, "Mẫu số 02 - VT", "PHIẾU XUẤT KHO", "Thực xuất",
                fileName("phieu-xuat-", document.code(), "xlsx"));
    }

    private ReceiptDocument buildImportDocument(ImportReceiptDraftResponse receipt) {
        List<ImportReceiptItemResponse> items = receipt.details() == null ? List.of() : receipt.details();
        Map<Long, String> unitByProductId = loadUnits(items.stream()
                .map(ImportReceiptItemResponse::productId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        List<DocumentLine> lines = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            ImportReceiptItemResponse item = items.get(index);
            lines.add(new DocumentLine(
                    index + 1,
                    value(item.productCode()),
                    value(item.productName()),
                    value(unitByProductId.get(item.productId())),
                    value(item.quantity()),
                    value(item.actualReceivedQuantity()),
                    money(item.unitPrice()),
                    money(item.lineTotal()),
                    value(item.note())));
        }

        return new ReceiptDocument(
                value(receipt.code()),
                firstDate(receipt.actualArrivalDate(), receipt.updatedAt()),
                value(receipt.warehouseName()),
                warehouseAddress(receipt.warehouseId()),
                value(receipt.supplierName()),
                partnerAddress(receipt.supplierId()),
                value(receipt.createdByName()),
                value(receipt.submittedByName()),
                value(receipt.status()),
                money(receipt.totalAmount()),
                VietnameseNumberToWords.currency(receipt.totalAmount()),
                value(receipt.note()),
                lines);
    }

    private ReceiptDocument buildExportDocument(ExportReceiptDetailResponse receipt) {
        List<ExportReceiptDetailItemResponse> items = receipt.items() == null ? List.of() : receipt.items();
        List<DocumentLine> lines = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            ExportReceiptDetailItemResponse item = items.get(index);
            lines.add(new DocumentLine(
                    index + 1,
                    value(item.productCode()),
                    value(item.productName()),
                    value(item.unit()),
                    value(item.quantity()),
                    value(item.quantity()),
                    money(item.unitPrice()),
                    money(item.lineTotal()),
                    value(item.note())));
        }

        return new ReceiptDocument(
                value(receipt.code()),
                receipt.createdAt() != null ? receipt.createdAt() : receipt.submittedAt(),
                value(receipt.warehouseName()),
                warehouseAddress(receipt.warehouseId()),
                value(receipt.partnerName()),
                partnerAddress(receipt.partnerId()),
                value(receipt.createdByName()),
                value(receipt.submittedByName()),
                value(receipt.status()),
                money(receipt.totalAmount()),
                VietnameseNumberToWords.currency(receipt.totalAmount()),
                value(receipt.note()),
                lines);
    }

    private GeneratedDocument renderPdf(ReceiptDocument document, String templateLabel, String title,
            String actualQuantityLabel, String filename) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document pdf = new Document(PageSize.A4, 24, 24, 24, 24);
            PdfWriter.getInstance(pdf, outputStream);
            pdf.open();

            Font regular = pdfFont("fonts/NotoSans.ttf", 9);
            Font bold = pdfFont("fonts/NotoSans-Bold.ttf", 9);
            Font titleFont = pdfFont("fonts/NotoSans-Bold.ttf", 14);

            Paragraph template = new Paragraph(templateLabel, bold);
            template.setAlignment(Element.ALIGN_CENTER);
            pdf.add(template);

            Paragraph heading = new Paragraph(title, titleFont);
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingAfter(6f);
            pdf.add(heading);

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.setSpacingAfter(6f);
            meta.setWidths(new float[] { 1.4f, 4.6f });
            addMetaRow(meta, "Số phiếu", document.code(), bold, regular);
            addMetaRow(meta, "Ngày", formatDate(document.documentDate()), bold, regular);
            addMetaRow(meta, "Trạng thái", document.status(), bold, regular);
            pdf.add(meta);

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setSpacingAfter(6f);
            header.setWidths(new float[] { 1.4f, 4.6f });
            addMetaRow(header, "Kho", document.warehouseName(), bold, regular);
            addMetaRow(header, "Địa chỉ kho", document.warehouseAddress(), bold, regular);
            addMetaRow(header, "Đối tác", document.partnerName(), bold, regular);
            addMetaRow(header, "Địa chỉ đối tác", document.partnerAddress(), bold, regular);
            addMetaRow(header, "Người tạo", document.createdBy(), bold, regular);
            addMetaRow(header, "Người gửi duyệt", document.submittedBy(), bold, regular);
            addMetaRow(header, "Bộ phận", "", bold, regular);
            addMetaRow(header, "Người giao hàng", "", bold, regular);
            addMetaRow(header, "Người nhận hàng", "", bold, regular);
            addMetaRow(header, "Số CT gốc", "", bold, regular);
            addMetaRow(header, "Nợ", "", bold, regular);
            addMetaRow(header, "Có", "", bold, regular);
            pdf.add(header);

            Paragraph note = new Paragraph("Ghi chú: " + document.note(), regular);
            note.setSpacingAfter(8f);
            pdf.add(note);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 0.8f, 1.5f, 2.6f, 1.1f, 1.2f, 1.2f, 1.3f, 1.5f, 2.2f });
            table.setHeaderRows(1);
            table.setSplitRows(true);
            table.setSplitLate(false);
            addHeaderCell(table, "STT", bold);
            addHeaderCell(table, "Mã SP", bold);
            addHeaderCell(table, "Tên hàng", bold);
            addHeaderCell(table, "ĐVT", bold);
            addHeaderCell(table, "Theo CT", bold);
            addHeaderCell(table, actualQuantityLabel, bold);
            addHeaderCell(table, "Đơn giá", bold);
            addHeaderCell(table, "Thành tiền", bold);
            addHeaderCell(table, "Ghi chú", bold);
            for (DocumentLine line : document.lines()) {
                addBodyCell(table, value(line.number()), regular, Element.ALIGN_CENTER);
                addBodyCell(table, line.productCode(), regular, Element.ALIGN_LEFT);
                addBodyCell(table, line.productName(), regular, Element.ALIGN_LEFT);
                addBodyCell(table, line.unit(), regular, Element.ALIGN_CENTER);
                addBodyCell(table, line.documentQuantity(), regular, Element.ALIGN_CENTER);
                addBodyCell(table, line.actualQuantity(), regular, Element.ALIGN_CENTER);
                addBodyCell(table, line.unitPrice(), regular, Element.ALIGN_RIGHT);
                addBodyCell(table, line.amount(), regular, Element.ALIGN_RIGHT);
                addBodyCell(table, line.note(), regular, Element.ALIGN_LEFT);
            }
            pdf.add(table);

            Paragraph totalWords = new Paragraph("Tổng số tiền (viết bằng chữ): " + document.totalInWords(), regular);
            totalWords.setSpacingBefore(6f);
            pdf.add(totalWords);

            PdfPTable sign = new PdfPTable(3);
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(12f);
            sign.setWidths(new float[] { 1f, 1f, 1f });
            sign.addCell(signatureCell("Người lập phiếu", bold, regular));
            sign.addCell(signatureCell("Thủ kho", bold, regular));
            sign.addCell(signatureCell("Kế toán trưởng", bold, regular));
            pdf.add(sign);

            pdf.close();
            return new GeneratedDocument(outputStream.toByteArray(), PDF_CONTENT_TYPE, filename);
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Khong the tao file PDF.", exception);
        }
    }

    private GeneratedDocument renderExcel(ReceiptDocument document, String templateLabel, String title,
            String actualQuantityLabel, String filename) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Phieu");
            sheet.setDisplayGridlines(false);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setLandscape(false);
            sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
            sheet.setMargin(Sheet.TopMargin, 0.5);
            sheet.setMargin(Sheet.BottomMargin, 0.5);
            sheet.setMargin(Sheet.LeftMargin, 0.35);
            sheet.setMargin(Sheet.RightMargin, 0.35);

            int[] widths = { 5, 14, 30, 10, 10, 12, 14, 16, 24 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            org.apache.poi.ss.usermodel.Font normalFont = workbook.createFont();
            normalFont.setFontName("Arial");
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setFontName("Arial");
            boldFont.setBold(true);

            CellStyle titleStyle = baseStyle(workbook, boldFont, HorizontalAlignment.CENTER, false);
            CellStyle labelStyle = baseStyle(workbook, boldFont, HorizontalAlignment.LEFT, false);
            CellStyle valueStyle = baseStyle(workbook, normalFont, HorizontalAlignment.LEFT, false);
            CellStyle centerStyle = baseStyle(workbook, normalFont, HorizontalAlignment.CENTER, false);
            CellStyle moneyStyle = baseStyle(workbook, normalFont, HorizontalAlignment.RIGHT, false);
            CellStyle headerStyle = baseStyle(workbook, boldFont, HorizontalAlignment.CENTER, true);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int rowIndex = 0;
            rowIndex = writeMergedTitle(sheet, rowIndex, templateLabel, title, titleStyle);
            rowIndex = writeMeta(sheet, document, rowIndex, labelStyle, valueStyle);

            Row header = sheet.createRow(rowIndex++);
            String[] headers = { "STT", "Mã SP", "Tên hàng", "ĐVT", "Theo CT", actualQuantityLabel, "Đơn giá",
                    "Thành tiền", "Ghi chú" };
            for (int i = 0; i < headers.length; i++) {
                writeCell(header, i, headers[i], headerStyle);
            }

            for (DocumentLine line : document.lines()) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, line.number(), centerStyle);
                writeCell(row, 1, line.productCode(), valueStyle);
                writeCell(row, 2, line.productName(), valueStyle);
                writeCell(row, 3, line.unit(), centerStyle);
                writeCell(row, 4, line.documentQuantity(), centerStyle);
                writeCell(row, 5, line.actualQuantity(), centerStyle);
                writeCell(row, 6, line.unitPrice(), moneyStyle);
                writeCell(row, 7, line.amount(), moneyStyle);
                writeCell(row, 8, line.note(), valueStyle);
            }

            rowIndex++;
            Row total = sheet.createRow(rowIndex++);
            writeCell(total, 6, "Tổng cộng", labelStyle);
            writeCell(total, 7, document.totalAmount(), moneyStyle);

            Row words = sheet.createRow(rowIndex++);
            writeCell(words, 0, "Tổng số tiền (viết bằng chữ)", labelStyle);
            writeCell(words, 1, document.totalInWords(), valueStyle);

            rowIndex++;
            Row sign = sheet.createRow(rowIndex++);
            writeCell(sign, 0, "Người lập phiếu", labelStyle);
            writeCell(sign, 3, "Thủ kho", labelStyle);
            writeCell(sign, 6, "Kế toán trưởng", labelStyle);

            workbook.write(outputStream);
            return new GeneratedDocument(outputStream.toByteArray(), XLSX_CONTENT_TYPE, filename);
        } catch (IOException exception) {
            throw new IllegalStateException("Khong the tao file Excel.", exception);
        }
    }

    private int writeMergedTitle(Sheet sheet, int rowIndex, String templateLabel, String title, CellStyle style) {
        Row row1 = sheet.createRow(rowIndex++);
        writeCell(row1, 0, templateLabel, style);
        mergeRow(sheet, row1.getRowNum(), 0, 8);
        Row row2 = sheet.createRow(rowIndex++);
        writeCell(row2, 0, title, style);
        mergeRow(sheet, row2.getRowNum(), 0, 8);
        return rowIndex;
    }

    private int writeMeta(Sheet sheet, ReceiptDocument document, int rowIndex, CellStyle labelStyle, CellStyle valueStyle) {
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Số phiếu", document.code(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Ngày", formatDate(document.documentDate()), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Trạng thái", document.status(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Kho", document.warehouseName(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Địa chỉ kho", document.warehouseAddress(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Đối tác", document.partnerName(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Địa chỉ đối tác", document.partnerAddress(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Người tạo", document.createdBy(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Người gửi duyệt", document.submittedBy(), labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Bộ phận", "", labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Người giao hàng", "", labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Người nhận hàng", "", labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Số CT gốc", "", labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Nợ", "", labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Có", "", labelStyle, valueStyle);
        rowIndex = writeLabelValueRow(sheet, rowIndex, "Ghi chú", document.note(), labelStyle, valueStyle);
        return rowIndex;
    }

    private int writeLabelValueRow(Sheet sheet, int rowIndex, String label, String value, CellStyle labelStyle,
            CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex++);
        writeCell(row, 0, label, labelStyle);
        mergeRow(sheet, row.getRowNum(), 0, 2);
        writeCell(row, 3, value, valueStyle);
        mergeRow(sheet, row.getRowNum(), 3, 8);
        return rowIndex;
    }

    private void mergeRow(Sheet sheet, int rowNum, int from, int to) {
        if (from < to) {
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum, from, to));
        }
    }

    private CellStyle baseStyle(Workbook workbook, org.apache.poi.ss.usermodel.Font font, HorizontalAlignment alignment,
            boolean wrap) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(wrap);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value(value));
        cell.setCellStyle(style);
    }

    private void writeCell(Row row, int index, Integer value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private void writeCell(Row row, int index, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(index);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private void addMetaRow(PdfPTable table, String label, String value, Font bold, Font regular) {
        PdfPCell left = new PdfPCell(new Phrase(label, bold));
        left.setPadding(4f);
        left.setBackgroundColor(new Color(240, 240, 240));
        table.addCell(left);
        PdfPCell right = new PdfPCell(new Phrase(value(value), regular));
        right.setPadding(4f);
        table.addCell(right);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new Color(230, 230, 230));
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value(text), font));
        cell.setPadding(4f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    private PdfPCell signatureCell(String title, Font bold, Font regular) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(90f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.addElement(new Paragraph(title, bold));
        cell.addElement(new Paragraph(" ", regular));
        return cell;
    }

    private Font pdfFont(String resourcePath, int size) throws IOException, DocumentException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            BaseFont baseFont = BaseFont.createFont(resource.getFilename(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true,
                    inputStream.readAllBytes(), null);
            return new Font(baseFont, size);
        }
    }

    private Map<Long, String> loadUnits(Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getUnit, (left, right) -> left, LinkedHashMap::new));
    }

    private String warehouseAddress(Long warehouseId) {
        if (warehouseId == null) {
            return "";
        }
        return warehouseRepository.findById(warehouseId).map(Warehouse::getAddress).orElse("");
    }

    private String partnerAddress(Long partnerId) {
        if (partnerId == null) {
            return "";
        }
        return partnerRepository.findById(partnerId).map(Partner::getAddress).orElse("");
    }

    private LocalDateTime firstDate(LocalDateTime first, LocalDateTime fallback) {
        return first != null ? first : fallback;
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT);
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return new java.text.DecimalFormat("#,##0").format(value.setScale(0, RoundingMode.HALF_UP));
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private String value(Integer number) {
        return number == null ? "" : String.valueOf(number);
    }

    private String fileName(String prefix, String code, String extension) {
        String safe = code == null || code.isBlank() ? "document" : code.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        return prefix + safe + "." + extension;
    }

    private record ReceiptDocument(
            String code,
            LocalDateTime documentDate,
            String warehouseName,
            String warehouseAddress,
            String partnerName,
            String partnerAddress,
            String createdBy,
            String submittedBy,
            String status,
            String totalAmount,
            String totalInWords,
            String note,
            List<DocumentLine> lines) {
    }

    private record DocumentLine(
            int number,
            String productCode,
            String productName,
            String unit,
            String documentQuantity,
            String actualQuantity,
            String unitPrice,
            String amount,
            String note) {
    }
}
