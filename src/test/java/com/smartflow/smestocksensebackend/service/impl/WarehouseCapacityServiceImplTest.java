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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
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
}
