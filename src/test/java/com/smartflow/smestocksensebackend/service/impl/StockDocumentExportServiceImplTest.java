package com.smartflow.smestocksensebackend.service.impl;

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
import com.smartflow.smestocksensebackend.service.document.GeneratedDocument;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockDocumentExportServiceImplTest {

    @Mock
    ImportReceiptService importReceiptService;
    @Mock
    ExportReceiptService exportReceiptService;
    @Mock
    ProductRepository productRepository;
    @Mock
    WarehouseRepository warehouseRepository;
    @Mock
    PartnerRepository partnerRepository;

    @Test
    void exportImportReceiptExcel_shouldRenderWorkbook() throws Exception {
        StockDocumentExportServiceImpl service = service();

        Product product = new Product();
        product.setId(10L);
        product.setUnit("Cái");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setAddress("Kho A");

        Partner partner = new Partner();
        partner.setId(2L);
        partner.setAddress("Địa chỉ NCC");

        List<ImportReceiptItemResponse> items = List.of(
                importItem(1L, "SP-01", "Sản phẩm 1", 5, 5, "1000", "5000"),
                importItem(2L, "SP-02", "Sản phẩm 2", 2, 2, "1000", "2000"),
                importItem(3L, "SP-03", "Sản phẩm 3", 3, 3, "1000", "3000"),
                importItem(4L, "SP-04", "Sản phẩm 4", 4, 4, "1000", "4000"),
                importItem(5L, "SP-05", "Sản phẩm 5", 1, 1, "1000", "1000"));

        when(importReceiptService.getDetail(1L)).thenReturn(new ImportReceiptDraftResponse(
                1L, "PN-01", 1L, "Kho A", 2L, "Nhà cung cấp", 3L, "Người lập",
                "HOAN_THANH", new BigDecimal("15000"), "Ghi chú", items, 5,
                LocalDateTime.now(), 1L));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(2L)).thenReturn(Optional.of(partner));

        GeneratedDocument document = service.exportImportReceiptExcel(1L);

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", document.contentType());
        assertEquals("phieu-nhap-pn-01.xlsx", document.filename());
        assertTrue(new ClassPathResource("templates/stock-documents/phieu-nhap-kho-tt133.xlsx").exists());
        assertTrue(document.content().length > 0);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(document.content()))) {
            var sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getRow(4).getCell(0).getStringCellValue().contains("PHIẾU NHẬP KHO"));
            assertTrue(sheet.getRow(6).getCell(2).getStringCellValue().contains("PN-01"));
            assertTrue(sheet.getRow(8).getCell(0).getStringCellValue().contains("Nhà cung cấp"));
            assertTrue(sheet.getRow(10).getCell(0).getStringCellValue().contains("Kho A"));
            assertEquals("Sản phẩm 1", sheet.getRow(15).getCell(1).getStringCellValue());
            assertEquals("SP-01", sheet.getRow(15).getCell(2).getStringCellValue());
            assertEquals(5, (int) sheet.getRow(15).getCell(4).getNumericCellValue());
            assertEquals(5, (int) sheet.getRow(15).getCell(5).getNumericCellValue());
            assertEquals(1000, (int) sheet.getRow(15).getCell(6).getNumericCellValue());
            assertEquals("Sản phẩm 5", sheet.getRow(19).getCell(1).getStringCellValue());
            assertEquals(15000, (int) sheet.getRow(20).getCell(7).getNumericCellValue());
        }
    }

    @Test
    void exportExportReceiptExcel_shouldUseTemplateAndExpandRows() throws Exception {
        StockDocumentExportServiceImpl service = service();
        List<ExportReceiptDetailItemResponse> items = List.of(
                exportItem(1L, "SP-01", "Sản phẩm 1", 3, "1000", "3000"),
                exportItem(2L, "SP-02", "Sản phẩm 2", 4, "1000", "4000"),
                exportItem(3L, "SP-03", "Sản phẩm 3", 5, "1000", "5000"),
                exportItem(4L, "SP-04", "Sản phẩm 4", 6, "1000", "6000"));

        when(exportReceiptService.getDetail(2L)).thenReturn(new ExportReceiptDetailResponse(
                2L, "PX-01", "Người lập", LocalDateTime.now(), LocalDateTime.now(), "Kho B", "Xuất bán",
                "DA_DUYET", "Cấp 1", null, null, null, items, 1L, "Kho B",
                2L, "Khách hàng", 3L, "Người lập", "Người gửi", new BigDecimal("18000"), 1L));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse("Kho B")));
        when(partnerRepository.findById(2L)).thenReturn(Optional.of(partner("Địa chỉ KH")));

        GeneratedDocument document = service.exportExportReceiptExcel(2L);

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", document.contentType());
        assertEquals("phieu-xuat-px-01.xlsx", document.filename());
        assertTrue(new ClassPathResource("templates/stock-documents/phieu-xuat-kho-tt133.xlsx").exists());
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(document.content()))) {
            var sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getRow(4).getCell(0).getStringCellValue().contains("PHIẾU XUẤT KHO"));
            assertTrue(sheet.getRow(6).getCell(0).getStringCellValue().contains("PX-01"));
            assertTrue(sheet.getRow(9).getCell(0).getStringCellValue().contains("Khách hàng"));
            assertTrue(sheet.getRow(11).getCell(0).getStringCellValue().contains("Kho B"));
            assertEquals("Sản phẩm 1", sheet.getRow(16).getCell(2).getStringCellValue());
            assertEquals("SP-01", sheet.getRow(16).getCell(3).getStringCellValue());
            assertEquals(3, (int) sheet.getRow(16).getCell(5).getNumericCellValue());
            assertEquals(3, (int) sheet.getRow(16).getCell(6).getNumericCellValue());
            assertEquals(3000, (int) sheet.getRow(16).getCell(8).getNumericCellValue());
            assertEquals("Sản phẩm 4", sheet.getRow(19).getCell(2).getStringCellValue());
            assertEquals(18000, (int) sheet.getRow(20).getCell(8).getNumericCellValue());
        }
    }

    @Test
    void exportExportReceiptPdf_shouldRenderPdfHeader() {
        StockDocumentExportServiceImpl service = service();

        ExportReceiptDetailItemResponse item = new ExportReceiptDetailItemResponse(
                1L, 10L, "SP-01", "Sản phẩm 1", "Cái", 3, 3, true,
                new BigDecimal("1000"), new BigDecimal("3000"), "Ghi chú");

        when(exportReceiptService.getDetail(2L)).thenReturn(new ExportReceiptDetailResponse(
                2L, "PX-01", "Người lập", LocalDateTime.now(), LocalDateTime.now(), "Kho A", "Ghi chú",
                "DA_DUYET", "Cấp 1", null, null, null, List.of(item), 1L, "Kho A",
                null, null, 3L, "Người lập", "Người gửi", new BigDecimal("3000"), 1L));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        GeneratedDocument document = service.exportExportReceiptPdf(2L);

        assertTrue(document.content().length > 0);
        assertArrayEquals(new byte[] { '%', 'P', 'D', 'F' }, slice(document.content(), 4));
    }

    private StockDocumentExportServiceImpl service() {
        return new StockDocumentExportServiceImpl(importReceiptService, exportReceiptService, productRepository,
                warehouseRepository, partnerRepository);
    }

    private ImportReceiptItemResponse importItem(Long id, String code, String name, int quantity, int actualQuantity,
            String unitPrice, String lineTotal) {
        return new ImportReceiptItemResponse(
                id, 1L, 10L, code, name, quantity, actualQuantity, new BigDecimal(unitPrice),
                new BigDecimal(lineTotal), "Ghi chú", null, null, null);
    }

    private ExportReceiptDetailItemResponse exportItem(Long id, String code, String name, int quantity,
            String unitPrice, String lineTotal) {
        return new ExportReceiptDetailItemResponse(
                id, 10L, code, name, "Cái", quantity, quantity, true,
                new BigDecimal(unitPrice), new BigDecimal(lineTotal), "Ghi chú");
    }

    private Warehouse warehouse(String address) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setAddress(address);
        return warehouse;
    }

    private Partner partner(String address) {
        Partner partner = new Partner();
        partner.setId(2L);
        partner.setAddress(address);
        return partner;
    }

    private byte[] slice(byte[] bytes, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }
}
