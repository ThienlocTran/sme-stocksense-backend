package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.event.InventoryLevelChangedEvent;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.AlertSeverityCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho luồng T184: xử lý event biến động tồn kho.
 * Kiểm tra 4 kịch bản chính: tạo mới, cập nhật, auto-resolve, và no-op.
 */
@ExtendWith(MockitoExtension.class)
class InventoryAlertDetectionServiceImplT184Test {

    @Mock
    private InventoryAlertRepository inventoryAlertRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private AlertSeverityCalculator alertSeverityCalculator;

    @InjectMocks
    private InventoryAlertDetectionServiceImpl service;

    // --- Kịch bản 1: Tồn kho thiếu hụt, chưa có cảnh báo -> Tạo mới ---
    @Test
    void processInventoryChange_WhenLowStockAndNoExistingAlert_ShouldCreateNewAlert() {
        // Given: Tồn kho 5, minStock 10 (thiếu hụt), chưa có cảnh báo nào
        InventoryLevelChangedEvent event = new InventoryLevelChangedEvent(1L, 2L, 20, 5, 10);
        when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                eq(2L), eq(1L), any())).thenReturn(Optional.empty());
        when(alertSeverityCalculator.calculate(anyInt(), anyString()))
                .thenReturn(InventoryAlertSeverity.WARNING);

        // When
        service.processInventoryChange(event);

        // Then: Phải lưu 1 cảnh báo mới
        verify(inventoryAlertRepository).save(any(InventoryAlert.class));
    }

    // --- Kịch bản 2: Tồn kho thiếu hụt, đã có cảnh báo OPEN -> Cập nhật số lượng (không tạo mới) ---
    @Test
    void processInventoryChange_WhenLowStockAndExistingOpenAlert_ShouldUpdateQuantityOnly() {
        // Given: Đã có cảnh báo OPEN với currentQuantity = 8
        InventoryLevelChangedEvent event = new InventoryLevelChangedEvent(1L, 2L, 8, 5, 10);
        InventoryAlert existingAlert = new InventoryAlert();
        existingAlert.setId(99L);
        existingAlert.setStatus(InventoryAlertStatus.OPEN);
        existingAlert.setCurrentQuantity(8);

        when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                eq(2L), eq(1L), any())).thenReturn(Optional.of(existingAlert));

        // When
        service.processInventoryChange(event);

        // Then: Chỉ cập nhật số lượng, không tạo thêm alert mới
        verify(inventoryAlertRepository, never()).save(any(InventoryAlert.class));
    }

    // --- Kịch bản 3: Tồn kho đủ hàng, đang có cảnh báo ACKNOWLEDGED -> Auto-Resolve ---
    @Test
    void processInventoryChange_WhenSufficientStockAndExistingAlert_ShouldAutoResolve() {
        // Given: Tồn kho 23 > minStock 10, đang có cảnh báo ACKNOWLEDGED
        InventoryLevelChangedEvent event = new InventoryLevelChangedEvent(1L, 2L, 3, 23, 10);
        InventoryAlert existingAlert = new InventoryAlert();
        existingAlert.setId(99L);
        existingAlert.setStatus(InventoryAlertStatus.ACKNOWLEDGED);
        existingAlert.setVersion(0L);

        when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                eq(2L), eq(1L), any())).thenReturn(Optional.of(existingAlert));

        // When
        service.processInventoryChange(event);

        // Then: Cảnh báo phải chuyển sang RESOLVED, handledBy = "System"
        verify(inventoryAlertRepository, never()).save(any(InventoryAlert.class)); // dirty checking
        assertEquals(InventoryAlertStatus.RESOLVED, existingAlert.getStatus());
        assertEquals("System", existingAlert.getHandledBy());
    }

    // --- Kịch bản 4: Tồn kho đủ hàng, không có cảnh báo nào -> Bỏ qua (no-op) ---
    @Test
    void processInventoryChange_WhenSufficientStockAndNoAlert_ShouldDoNothing() {
        // Given: Tồn kho 23 > minStock 10, không có cảnh báo nào
        InventoryLevelChangedEvent event = new InventoryLevelChangedEvent(1L, 2L, 5, 23, 10);
        when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                eq(2L), eq(1L), any())).thenReturn(Optional.empty());

        // When
        service.processInventoryChange(event);

        // Then: Không có bất kỳ thao tác ghi nào
        verify(inventoryAlertRepository, never()).save(any());
    }
}
