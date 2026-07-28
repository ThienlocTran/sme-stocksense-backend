package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.AlertDetectionResultResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử tự động cho InventoryAlertDetectionServiceImpl bằng Mockito.
 * Kiểm chứng đầy đủ các kịch bản Deduplication: tạo mới, không đổi (unchanged),
 * tụt kho sâu hơn (updated),
 * và bắt ngoại lệ Race Condition (raceConditionIgnored).
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
        @DisplayName("scanAndCreateAlerts: Xử lý chính xác vừa tạo mới phiếu vừa ghi nhận phiếu cũ giữ nguyên (unchanged)")
        void testScanAndCreateAlerts_WithNewAndExistingAlerts() {
                // Giả lập kho 10L có 2 mặt hàng tụt kho: 101L (đã có phiếu, số lượng không đổi)
                // và 102L (chưa có phiếu)
                InventoryLevelProjection unchangedItem = mockCreatedProjection(101L, 10L, 10, 20, "LOW_STOCK");
                InventoryLevelProjection createdItem = mockCreatedProjection(102L, 10L, 5, 20, "LOW_STOCK");

                when(inventoryLevelRepository.findInventory(
                                eq(10L), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(unchangedItem, createdItem)));

                // Mặt hàng 101L đã có phiếu OPEN với số lượng cũ = 10 -> không đổi
                InventoryAlert existingAlert = mockAlert(1001L, 101L, 10L, 10);
                when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                                eq(101L), eq(10L), anyList())).thenReturn(Optional.of(existingAlert));

                // Mặt hàng 102L chưa có phiếu -> tạo mới
                when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                                eq(102L), eq(10L), anyList())).thenReturn(Optional.empty());

                AlertDetectionResultResponse result = detectionService.scanAndCreateAlerts(10L);

                assertEquals(2, result.totalScanned(), "Tổng số mặt hàng quét phải là 2");
                assertEquals(1, result.newAlertsCreated(), "Chỉ tạo mới 1 phiếu cho mặt hàng 102L");
                assertEquals(1, result.existingAlertsUnchanged(), "Ghi nhận 1 mặt hàng 101L giữ nguyên");
                assertEquals(0, result.existingAlertsUpdated());
                assertEquals(0, result.raceConditionIgnored());
                assertEquals(1, result.existingAlertsSkipped(), "Tổng skipped = unchanged + race = 1");
                assertNotNull(result.timestamp());

                // Xác minh save chỉ được gọi cho mặt hàng 102L (tạo mới)
                ArgumentCaptor<InventoryAlert> captor = ArgumentCaptor.forClass(InventoryAlert.class);
                verify(inventoryAlertRepository, times(1)).save(captor.capture());

                InventoryAlert savedAlert = captor.getValue();
                assertEquals(102L, savedAlert.getProduct().getId());
                assertEquals(5, savedAlert.getCurrentQuantity());
                assertEquals(InventoryAlertStatus.OPEN, savedAlert.getStatus());
                assertNull(savedAlert.getHandledBy());
        }

        @Test
        @DisplayName("testDeduplication: Cập nhật currentQuantity khi tồn kho tụt sâu hơn trước (newQty < oldQty)")
        void testDeduplication_WhenStockDropsFurther_ShouldUpdateQuantity() {
                // Mặt hàng 103L có phiếu cũ tồn = 15, nay quét thấy tụt xuống còn 4
                InventoryLevelProjection droppedItem = mockCreatedProjection(103L, 10L, 4, 20, "LOW_STOCK");

                when(inventoryLevelRepository.findInventory(
                                eq(10L), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(droppedItem)));

                InventoryAlert existingAlert = mockAlert(1002L, 103L, 10L, 15);
                when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                                eq(103L), eq(10L), anyList())).thenReturn(Optional.of(existingAlert));

                AlertDetectionResultResponse result = detectionService.scanAndCreateAlerts(10L);

                assertEquals(1, result.totalScanned());
                assertEquals(0, result.newAlertsCreated());
                assertEquals(1, result.existingAlertsUpdated(),
                                "Phải ghi nhận 1 phiếu được cập nhật số lượng tụt sâu hơn");
                assertEquals(0, result.existingAlertsUnchanged());
                assertEquals(4, existingAlert.getCurrentQuantity(), "Số lượng của phiếu cũ phải được cập nhật thành 4");

                verify(inventoryAlertRepository, times(1)).save(existingAlert);
        }

        @Test
        @DisplayName("testDeduplication: Bắt ngoại lệ DataIntegrityViolationException khi Race Condition và ghi nhận raceConditionIgnored")
        void testDeduplication_RaceCondition_ShouldCatchExceptionAndIgnore() {
                InventoryLevelProjection raceItem = mockCreatedProjection(104L, 10L, 2, 20, "LOW_STOCK");

                when(inventoryLevelRepository.findInventory(
                                eq(10L), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(raceItem)));

                when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                                eq(104L), eq(10L), anyList())).thenReturn(Optional.empty());

                // Giả lập giao dịch khác vừa tạo phiếu trước 1 mili-giây -> JPA/DB ném
                // DataIntegrityViolationException
                when(inventoryAlertRepository.save(any(InventoryAlert.class)))
                                .thenThrow(new DataIntegrityViolationException(
                                                "Unique index idx_unique_active_alert violation"));

                AlertDetectionResultResponse result = detectionService.scanAndCreateAlerts(10L);

                assertEquals(1, result.totalScanned());
                assertEquals(0, result.newAlertsCreated());
                assertEquals(1, result.raceConditionIgnored(), "Phải đếm 1 vào nhóm raceConditionIgnored");
                assertEquals(1, result.existingAlertsSkipped());
        }

        @Test
        @DisplayName("scanAndCreateAlerts: Trả về kết quả rỗng (0, 0, 0, 0, 0) khi không phát hiện mặt hàng tụt kho")
        void testScan_NoLowStock_ReturnEmptyResult() {
                when(inventoryLevelRepository.findInventory(
                                eq(10L), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of()));

                AlertDetectionResultResponse result = detectionService.scanAndCreateAlerts(10L);

                assertEquals(0, result.totalScanned());
                assertEquals(0, result.newAlertsCreated());
                assertEquals(0, result.existingAlertsUpdated());
                assertEquals(0, result.existingAlertsUnchanged());
                assertEquals(0, result.raceConditionIgnored());

                verifyNoInteractions(inventoryAlertRepository);
        }

        @Test
        @DisplayName("checkAndCreateAlert: Kiểm tra điểm (Spot Check) tạo phiếu khi tụt kho và từ chối khi bình thường/trùng lặp không đổi")
        void testCheckAndCreateAlert_LowStockAndNormal() {
                // Kịch bản 1: Mặt hàng tụt kho, chưa có phiếu -> trả về true và tạo phiếu
                InventoryLevelProjection item = mockCreatedProjection(200L, 20L, 0, 10, "OUT_OF_STOCK");
                when(inventoryLevelRepository.findInventory(
                                eq(20L), eq(200L), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(item)));

                when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                                eq(200L), eq(20L), anyList())).thenReturn(Optional.empty());

                boolean created = detectionService.checkAndCreateAlert(200L, 20L);
                assertTrue(created, "Phải trả về true khi đã sinh phiếu cảnh báo mới");
                verify(inventoryAlertRepository, times(1)).save(any(InventoryAlert.class));

                // Kịch bản 2: Mặt hàng bình thường -> trả về false
                when(inventoryLevelRepository.findInventory(
                                eq(20L), eq(300L), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of()));

                boolean notCreated = detectionService.checkAndCreateAlert(300L, 20L);
                assertFalse(notCreated, "Phải trả về false khi mặt hàng không bị tụt kho");

                // Kịch bản 3: Mặt hàng tụt kho nhưng đã có phiếu với số lượng không đổi -> trả
                // về false
                InventoryLevelProjection skippedItem400 = mockCreatedProjection(400L, 20L, 5, 20, "LOW_STOCK");
                when(inventoryLevelRepository.findInventory(
                                eq(20L), eq(400L), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"),
                                any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(skippedItem400)));

                InventoryAlert alert400 = mockAlert(1003L, 400L, 20L, 5);
                when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                                eq(400L), eq(20L), anyList())).thenReturn(Optional.of(alert400));

                boolean skipped = detectionService.checkAndCreateAlert(400L, 20L);
                assertFalse(skipped,
                                "Phải trả về false khi mặt hàng đã có phiếu OPEN/ACKNOWLEDGED và số lượng không đổi");
        }

        private InventoryLevelProjection mockCreatedProjection(Long productId, Long warehouseId, Integer qty,
                        Integer minStock, String status) {
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

        private InventoryAlert mockAlert(Long alertId, Long productId, Long warehouseId, Integer currentQty) {
                Product prod = new Product();
                prod.setId(productId);
                Warehouse wh = new Warehouse();
                wh.setId(warehouseId);

                return InventoryAlert.builder()
                                .id(alertId)
                                .product(prod)
                                .warehouse(wh)
                                .currentQuantity(currentQty)
                                .minStock(20)
                                .maxStock(100)
                                .severity(InventoryAlertSeverity.WARNING)
                                .status(InventoryAlertStatus.OPEN)
                                .build();
        }
}
