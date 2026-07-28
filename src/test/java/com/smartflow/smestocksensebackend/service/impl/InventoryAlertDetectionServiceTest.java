package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.AlertDetectionResultResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Note: [T178 - Unit Test] Kiểm thử tự động cho InventoryAlertDetectionServiceImpl bằng Mockito.
 * Không cần DB thực, chạy nhanh trên CI/Maven theo chuẩn Ponytail.
 * Kiểm chứng đầy đủ 3 kịch bản: Quét có hàng mới & cũ, Quét kho bình thường (rỗng), và Spot Check.
 */
@ExtendWith(MockitoExtension.class)
class InventoryAlertDetectionServiceTest {

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private InventoryAlertRepository inventoryAlertRepository;

    @InjectMocks
    private InventoryAlertDetectionServiceImpl detectionService;

    @Test
    @DisplayName("scanAndCreateAlerts: Xử lý chính xác vừa tạo mới phiếu (hàng chưa có phiếu) vừa bỏ qua (hàng đã có phiếu)")
    void testScanAndCreateAlerts_WithNewAndExistingAlerts() {
        // Giả lập kho 10L có 2 mặt hàng đang tụt kho
        InventoryLevelProjection skippedItem = mockSkippedProjection(101L, 10L);
        InventoryLevelProjection createdItem = mockCreatedProjection(102L, 10L, 5, 20, "LOW_STOCK");

        when(inventoryLevelRepository.findInventory(
                eq(10L), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(skippedItem, createdItem)));

        // Mặt hàng 101L đã có phiếu OPEN/ACKNOWLEDGED -> bỏ qua
        when(inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(
                eq(101L), eq(10L), anyList())).thenReturn(true);

        // Mặt hàng 102L chưa có phiếu -> tạo mới
        when(inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(
                eq(102L), eq(10L), anyList())).thenReturn(false);

        AlertDetectionResultResponse result = detectionService.scanAndCreateAlerts(10L);

        assertEquals(2, result.totalScanned(), "Tổng số mặt hàng quét phải là 2");
        assertEquals(1, result.newAlertsCreated(), "Chỉ tạo mới 1 phiếu cho mặt hàng 102L");
        assertEquals(1, result.existingAlertsSkipped(), "Bỏ qua 1 mặt hàng 101L do trùng lặp");
        assertNotNull(result.timestamp());

        // Xác minh save được gọi chính xác 1 lần cho mặt hàng 102L
        ArgumentCaptor<InventoryAlert> captor = ArgumentCaptor.forClass(InventoryAlert.class);
        verify(inventoryAlertRepository, times(1)).save(captor.capture());

        InventoryAlert savedAlert = captor.getValue();
        assertEquals(102L, savedAlert.getProduct().getId());
        assertEquals(10L, savedAlert.getWarehouse().getId());
        assertEquals(5, savedAlert.getCurrentQuantity());
        assertEquals(InventoryAlertSeverity.WARNING, savedAlert.getSeverity());
        assertEquals(InventoryAlertStatus.OPEN, savedAlert.getStatus());
        assertNull(savedAlert.getHandledBy());
    }

    @Test
    @DisplayName("scanAndCreateAlerts: Trả về kết quả rỗng (0, 0, 0) khi không phát hiện mặt hàng tụt kho")
    void testScan_NoLowStock_ReturnEmptyResult() {
        when(inventoryLevelRepository.findInventory(
                eq(10L), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of())); // Không có hàng nào dưới định mức

        AlertDetectionResultResponse result = detectionService.scanAndCreateAlerts(10L);

        assertEquals(0, result.totalScanned());
        assertEquals(0, result.newAlertsCreated());
        assertEquals(0, result.existingAlertsSkipped());

        // Xác minh không bao giờ gọi xuống InventoryAlertRepository
        verifyNoInteractions(inventoryAlertRepository);
    }

    @Test
    @DisplayName("checkAndCreateAlert: Kiểm tra điểm (Spot Check) tạo phiếu khi tụt kho và từ chối khi bình thường/trùng lặp")
    void testCheckAndCreateAlert_LowStockAndNormal() {
        // Kịch bản 1: Mặt hàng tụt kho, chưa có phiếu -> trả về true và tạo phiếu
        InventoryLevelProjection item = mockCreatedProjection(200L, 20L, 0, 10, "OUT_OF_STOCK");
        when(inventoryLevelRepository.findInventory(
                eq(20L), eq(200L), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        when(inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(
                eq(200L), eq(20L), anyList())).thenReturn(false);

        boolean created = detectionService.checkAndCreateAlert(200L, 20L);
        assertTrue(created, "Phải trả về true khi đã sinh phiếu cảnh báo mới");
        verify(inventoryAlertRepository, times(1)).save(any(InventoryAlert.class));

        // Kịch bản 2: Mặt hàng bình thường (query trả về rỗng) -> trả về false
        when(inventoryLevelRepository.findInventory(
                eq(20L), eq(300L), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        boolean notCreated = detectionService.checkAndCreateAlert(300L, 20L);
        assertFalse(notCreated, "Phải trả về false khi mặt hàng không bị tụt kho");

        // Kịch bản 3: Mặt hàng tụt kho nhưng đã có phiếu từ trước -> trả về false
        InventoryLevelProjection skippedItem400 = mockSkippedProjection(400L, 20L);
        when(inventoryLevelRepository.findInventory(
                eq(20L), eq(400L), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(skippedItem400)));

        when(inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(
                eq(400L), eq(20L), anyList())).thenReturn(true);

        boolean skipped = detectionService.checkAndCreateAlert(400L, 20L);
        assertFalse(skipped, "Phải trả về false khi mặt hàng đã có phiếu OPEN/ACKNOWLEDGED từ trước");
    }

    private InventoryLevelProjection mockSkippedProjection(Long productId, Long warehouseId) {
        InventoryLevelProjection p = mock(InventoryLevelProjection.class);
        lenient().when(p.getProductId()).thenReturn(productId);
        lenient().when(p.getWarehouseId()).thenReturn(warehouseId);
        return p;
    }

    private InventoryLevelProjection mockCreatedProjection(Long productId, Long warehouseId, Integer qty, Integer minStock, String status) {
        InventoryLevelProjection p = mock(InventoryLevelProjection.class);
        lenient().when(p.getProductId()).thenReturn(productId);
        lenient().when(p.getWarehouseId()).thenReturn(warehouseId);
        lenient().when(p.getProductCode()).thenReturn("SP_" + productId);
        lenient().when(p.getProductName()).thenReturn("Product " + productId);
        lenient().when(p.getWarehouseCode()).thenReturn("K_" + warehouseId);
        lenient().when(p.getWarehouse()).thenReturn("Warehouse " + warehouseId);
        lenient().when(p.getCurrentQuantity()).thenReturn(qty);
        lenient().when(p.getMinStock()).thenReturn(minStock);
        lenient().when(p.getMaxStock()).thenReturn(100);
        lenient().when(p.getStatus()).thenReturn(status);
        return p;
    }
}
