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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

        ImportReceiptItemResponse item = new ImportReceiptItemResponse(
                1L, 1L, 10L, "SP-01", "Sản phẩm 1", 5, 5, new BigDecimal("1000"),
                new BigDecimal("5000"), "Ghi chú", null, null, null);

        when(importReceiptService.getDetail(1L)).thenReturn(new ImportReceiptDraftResponse(
                1L, "PN-01", 1L, "Kho A", 2L, "Nhà cung cấp", 3L, "Người lập",
                "HOAN_THANH", new BigDecimal("5000"), "Ghi chú", List.of(item), 1,
                LocalDateTime.now(), 1L));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(2L)).thenReturn(Optional.of(partner));

        GeneratedDocument document = service.exportImportReceiptExcel(1L);

        assertTrue(document.content().length > 0);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(document.content()))) {
            assertTrue(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue().contains("PHIẾU NHẬP KHO"));
            assertTrue(workbook.getSheetAt(0).getRow(19).getCell(2).getStringCellValue().contains("Sản phẩm 1"));
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

    private byte[] slice(byte[] bytes, int length) {
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0, length);
        return result;
    }
}
