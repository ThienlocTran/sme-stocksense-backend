package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseCapacityAlertRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseCapacityServiceImplTest {

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseStockConfigRepository warehouseStockConfigRepository;

    @Mock
    private WarehouseCapacityAlertRepository warehouseCapacityAlertRepository;

    @InjectMocks
    private WarehouseCapacityServiceImpl service;

    @Test
    void getUsedCapacity_shouldUseAggregateQuery() {
        when(inventoryLevelRepository.sumUsedCapacityByWarehouseId(anyLong()))
                .thenReturn(new BigDecimal("12.345"));

        assertEquals(new BigDecimal("12.345"), service.getUsedCapacity(1L));
    }

    @Test
    void getMinimumSafeVolume_shouldUseAggregateQuery() {
        when(warehouseStockConfigRepository.sumMinimumSafeVolumeByWarehouseId(anyLong()))
                .thenReturn(new BigDecimal("9.876"));

        assertEquals(new BigDecimal("9.876"), service.getMinimumSafeVolume(1L));
    }

    @Test
    void availability_shouldUseAllProductsInWarehouseAggregate() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse("100.000")));
        when(inventoryLevelRepository.sumUsedCapacityByWarehouseId(1L)).thenReturn(new BigDecimal("35.000"));

        var result = service.getAvailability(1L, new BigDecimal("2.000"));

        assertEquals(new BigDecimal("100.000"), result.warehouseCapacityM3());
        assertEquals(new BigDecimal("35.000"), result.warehouseOccupiedM3());
        assertEquals(new BigDecimal("65.000"), result.warehouseAvailableM3());
        assertEquals(32, result.maxAdditionalUnitsByCapacity());
        verify(inventoryLevelRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void availability_fullWarehouseAllowsZeroUnits() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse("100.000")));
        when(inventoryLevelRepository.sumUsedCapacityByWarehouseId(1L)).thenReturn(new BigDecimal("100.000"));

        var result = service.getAvailability(1L, new BigDecimal("2.000"));

        assertEquals(new BigDecimal("0.000"), result.warehouseAvailableM3());
        assertEquals(0, result.maxAdditionalUnitsByCapacity());
    }

    @Test
    void availability_overCapacityClampsAvailableToZero() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse("100.000")));
        when(inventoryLevelRepository.sumUsedCapacityByWarehouseId(1L)).thenReturn(new BigDecimal("120.000"));

        var result = service.getAvailability(1L, new BigDecimal("2.000"));

        assertEquals(new BigDecimal("0.000"), result.warehouseAvailableM3());
        assertEquals(0, result.maxAdditionalUnitsByCapacity());
        assertEquals("WAREHOUSE_OVER_CAPACITY", result.configurationWarning());
    }

    @Test
    void availability_usesFloorForTargetProductUnitVolume() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse("10.000")));
        when(inventoryLevelRepository.sumUsedCapacityByWarehouseId(1L)).thenReturn(new BigDecimal("0.000"));

        var result = service.getAvailability(1L, new BigDecimal("3.000"));

        assertEquals(3, result.maxAdditionalUnitsByCapacity());
    }

    @Test
    void availability_rejectsInvalidProductVolume() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse("10.000")));

        assertThrows(com.smartflow.smestocksensebackend.exception.BadRequestException.class,
                () -> service.getAvailability(1L, BigDecimal.ZERO));
    }

    @Test
    void availability_rejectsInvalidWarehouseCapacity() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(null)));

        assertThrows(com.smartflow.smestocksensebackend.exception.BadRequestException.class,
                () -> service.getAvailability(1L, BigDecimal.ONE));
    }

    private com.smartflow.smestocksensebackend.entity.Warehouse warehouse(String maxCapacity) {
        com.smartflow.smestocksensebackend.entity.Warehouse warehouse = new com.smartflow.smestocksensebackend.entity.Warehouse();
        warehouse.setMaxCapacityM3(maxCapacity == null ? null : new BigDecimal(maxCapacity));
        return warehouse;
    }
}
