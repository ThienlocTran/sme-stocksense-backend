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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
    private static final String IMPORT_TEMPLATE = "templates/stock-documents/phieu-nhap-kho-tt133.xlsx";
    private static final String EXPORT_TEMPLATE = "templates/stock-documents/phieu-xuat-kho-tt133.xlsx";
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
                IMPORT_TEMPLATE, true, fileName("phieu-nhap-", document.code(), "xlsx"));
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
                EXPORT_TEMPLATE, false, fileName("phieu-xuat-", document.code(), "xlsx"));
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
                    item.quantity(),
                    item.actualReceivedQuantity(),
                    item.unitPrice(),
                    item.lineTotal(),
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
                receipt.totalAmount(),
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
                    item.quantity(),
                    item.quantity(),
                    item.unitPrice(),
                    item.lineTotal(),
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
                receipt.totalAmount(),
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
                addBodyCell(table, value(line.documentQuantity()), regular, Element.ALIGN_CENTER);
                addBodyCell(table, value(line.actualQuantity()), regular, Element.ALIGN_CENTER);
                addBodyCell(table, money(line.unitPrice()), regular, Element.ALIGN_RIGHT);
                addBodyCell(table, money(line.amount()), regular, Element.ALIGN_RIGHT);
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
            String actualQuantityLabel, String templatePath, boolean importDocument, String filename) {
        ClassPathResource template = new ClassPathResource(templatePath);
        try (InputStream inputStream = template.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            if (importDocument) {
                populateImportTemplate(sheet, document);
            } else {
                populateExportTemplate(sheet, document);
            }

            workbook.write(outputStream);
            return new GeneratedDocument(outputStream.toByteArray(), XLSX_CONTENT_TYPE, filename);
        } catch (IOException exception) {
            throw new IllegalStateException("Khong the tao file Excel.", exception);
        }
    }

    private void populateImportTemplate(Sheet sheet, ReceiptDocument document) {
        writeCell(sheet, 5, 2, "Ngày " + dateOnly(document.documentDate()));
        writeCell(sheet, 6, 2, "Số: " + document.code());
        writeCell(sheet, 8, 0, "- Họ và tên người giao: " + document.partnerName());
        writeCell(sheet, 10, 0, "- Nhập tại kho: " + document.warehouseName()
                + "    địa điểm " + document.warehouseAddress());
        writeLines(sheet, document.lines(), 15, 18, new int[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        int totalRow = 18 + Math.max(0, document.lines().size() - 3);
        writeCell(sheet, totalRow, 7, document.totalAmount());
        writeCell(sheet, totalRow + 2, 0, "- Tổng số tiền (viết bằng chữ): " + document.totalInWords());
    }

    private void populateExportTemplate(Sheet sheet, ReceiptDocument document) {
        writeCell(sheet, 5, 0, "Ngày " + dateOnly(document.documentDate()));
        writeCell(sheet, 6, 0, "             Số: " + document.code());
        writeCell(sheet, 9, 0, "- Họ và tên người nhận hàng: " + document.partnerName());
        writeCell(sheet, 10, 0, "- Lý do xuất kho: " + document.note());
        writeCell(sheet, 11, 0, "- Xuất tại kho (ngăn lô): " + document.warehouseName()
                + "    Địa điểm " + document.warehouseAddress());
        writeLines(sheet, document.lines(), 16, 19, new int[] { 0, 2, 3, 4, 5, 6, 7, 8 });
        int totalRow = 19 + Math.max(0, document.lines().size() - 3);
        writeCell(sheet, totalRow, 8, document.totalAmount());
        writeCell(sheet, totalRow + 2, 0, "- Tổng số tiền (viết bằng chữ): " + document.totalInWords());
    }

    private void writeLines(Sheet sheet, List<DocumentLine> lines, int firstRow, int totalRow, int[] columns) {
        int rowsNeeded = Math.max(lines.size(), totalRow - firstRow);
        int templateRows = totalRow - firstRow;
        if (rowsNeeded > templateRows) {
            sheet.shiftRows(totalRow, sheet.getLastRowNum(), rowsNeeded - templateRows, true, false);
            for (int rowIndex = firstRow + templateRows; rowIndex < firstRow + rowsNeeded; rowIndex++) {
                copyRowStyle(sheet.getRow(firstRow), getOrCreateRow(sheet, rowIndex));
            }
        }
        for (int i = 0; i < rowsNeeded; i++) {
            Row row = getOrCreateRow(sheet, firstRow + i);
            if (i < lines.size()) {
                DocumentLine line = lines.get(i);
                writeCell(row, columns[0], line.number());
                writeCell(row, columns[1], line.productName());
                writeCell(row, columns[2], line.productCode());
                writeCell(row, columns[3], line.unit());
                writeCell(row, columns[4], line.documentQuantity());
                writeCell(row, columns[5], line.actualQuantity());
                writeCell(row, columns[6], line.unitPrice());
                writeCell(row, columns[7], line.amount());
            } else {
                for (int column : columns) {
                    writeCell(row, column, "");
                }
            }
        }
    }

    private void copyRowStyle(Row source, Row target) {
        if (source == null || target == null) {
            return;
        }
        target.setHeight(source.getHeight());
        for (int i = 0; i < source.getLastCellNum(); i++) {
            Cell sourceCell = source.getCell(i);
            if (sourceCell != null) {
                Cell targetCell = getOrCreateCell(target, i);
                targetCell.setCellStyle(sourceCell.getCellStyle());
            }
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private Cell getOrCreateCell(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? row.createCell(index) : cell;
    }

    private void writeCell(Sheet sheet, int rowIndex, int columnIndex, String value) {
        writeCell(getOrCreateRow(sheet, rowIndex), columnIndex, value);
    }

    private void writeCell(Sheet sheet, int rowIndex, int columnIndex, BigDecimal value) {
        writeCell(getOrCreateRow(sheet, rowIndex), columnIndex, value);
    }

    private void writeCell(Row row, int index, String value) {
        getOrCreateCell(row, index).setCellValue(value(value));
    }

    private void writeCell(Row row, int index, Integer value) {
        Cell cell = getOrCreateCell(row, index);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value);
        }
    }

    private void writeCell(Row row, int index, BigDecimal value) {
        Cell cell = getOrCreateCell(row, index);
        if (value == null) {
            cell.setBlank();
        } else {
            cell.setCellValue(value.doubleValue());
        }
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

    private String dateOnly(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        LocalDate date = value.toLocalDate();
        return date.getDayOfMonth() + " tháng " + date.getMonthValue() + " năm " + date.getYear();
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
            BigDecimal totalAmount,
            String totalInWords,
            String note,
            List<DocumentLine> lines) {
    }

    private record DocumentLine(
            int number,
            String productCode,
            String productName,
            String unit,
            Integer documentQuantity,
            Integer actualQuantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            String note) {
    }
}
