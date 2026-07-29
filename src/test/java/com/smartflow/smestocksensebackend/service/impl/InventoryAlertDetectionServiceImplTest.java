package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.AlertDetectionResultResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.AlertSeverityCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAlertDetectionServiceImplTest {

    @Mock
    private InventoryAlertRepository inventoryAlertRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private AlertSeverityCalculator alertSeverityCalculator;

    @InjectMocks
    private InventoryAlertDetectionServiceImpl service;

    // --- Batch Check: scanAndCreateAlerts ---

    @Test
    void scanAndCreateAlerts_WhenNoLowStockItem_ShouldReturnEmptyResult() {
        // Given
        Long warehouseId = 1L;
        Page<InventoryLevelProjection> emptyPage = new PageImpl<>(List.of());
        when(inventoryLevelRepository.findInventory(
                eq(warehouseId), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)
        )).thenReturn(emptyPage);

        // When
        AlertDetectionResultResponse result = service.scanAndCreateAlerts(warehouseId);

        // Then
        assertEquals(0, result.totalScanned());
        assertEquals(0, result.newAlertsCreated());
        assertEquals(0, result.existingAlertsSkipped());
        verify(inventoryAlertRepository, never()).save(any());
    }

    @Test
    void scanAndCreateAlerts_WhenTwoLowStockItems_ShouldCreateTwoAlerts() {
        // Given
        Long warehouseId = 1L;
        InventoryLevelProjection proj1 = mock(InventoryLevelProjection.class);
        when(proj1.getWarehouseId()).thenReturn(warehouseId);
        when(proj1.getProductId()).thenReturn(101L);
        when(proj1.getCurrentQuantity()).thenReturn(5);
        when(proj1.getMinStock()).thenReturn(10);
        
        InventoryLevelProjection proj2 = mock(InventoryLevelProjection.class);
        when(proj2.getWarehouseId()).thenReturn(warehouseId);
        when(proj2.getProductId()).thenReturn(102L);
        when(proj2.getCurrentQuantity()).thenReturn(0);
        when(proj2.getMinStock()).thenReturn(5);

        Page<InventoryLevelProjection> page = new PageImpl<>(List.of(proj1, proj2));
        when(inventoryLevelRepository.findInventory(
                eq(warehouseId), isNull(), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)
        )).thenReturn(page);

        when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(anyLong(), anyLong(), any(Collection.class)))
                .thenReturn(Optional.empty()); // No existing alerts

        when(alertSeverityCalculator.calculate(anyInt(), nullable(String.class))).thenReturn(InventoryAlertSeverity.WARNING);

        // When
        AlertDetectionResultResponse result = service.scanAndCreateAlerts(warehouseId);

        // Then
        assertEquals(2, result.totalScanned());
        assertEquals(2, result.newAlertsCreated());
        assertEquals(0, result.existingAlertsSkipped());
        verify(inventoryAlertRepository, times(2)).save(any(InventoryAlert.class));
    }

    // --- Spot Check: checkAndCreateAlert ---

    @Test
    void checkAndCreateAlert_WhenNormalStock_ShouldReturnFalse() {
        // Given
        Long warehouseId = 1L;
        Long productId = 101L;
        
        Page<InventoryLevelProjection> emptyPage = new PageImpl<>(List.of());
        when(inventoryLevelRepository.findInventory(
                eq(warehouseId), eq(productId), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)
        )).thenReturn(emptyPage);

        // When
        boolean result = service.checkAndCreateAlert(productId, warehouseId);

        // Then
        assertFalse(result);
        verify(inventoryAlertRepository, never()).save(any());
    }

    @Test
    void checkAndCreateAlert_WhenLowStock_ShouldReturnTrue() {
        // Given
        Long warehouseId = 1L;
        Long productId = 101L;
        
        InventoryLevelProjection proj1 = mock(InventoryLevelProjection.class);
        when(proj1.getWarehouseId()).thenReturn(warehouseId);
        when(proj1.getProductId()).thenReturn(productId);
        when(proj1.getCurrentQuantity()).thenReturn(5);
        when(proj1.getMinStock()).thenReturn(10);
        
        Page<InventoryLevelProjection> page = new PageImpl<>(List.of(proj1));
        when(inventoryLevelRepository.findInventory(
                eq(warehouseId), eq(productId), isNull(), eq("LOW_STOCK"), eq("HOAT_DONG"), eq("HOAT_DONG"), any(Pageable.class)
        )).thenReturn(page);
                
        when(inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(anyLong(), anyLong(), any(Collection.class)))
                .thenReturn(Optional.empty());
                
        when(alertSeverityCalculator.calculate(anyInt(), nullable(String.class))).thenReturn(InventoryAlertSeverity.WARNING);

        // When
        boolean result = service.checkAndCreateAlert(productId, warehouseId);

        // Then
        assertTrue(result);
        verify(inventoryAlertRepository, times(1)).save(any(InventoryAlert.class));
    }
}
