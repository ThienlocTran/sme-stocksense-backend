package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.dto.replenishment.*;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplenishmentSuggestionServiceImplTest {
    @Mock InventoryLevelRepository repository;
    @Mock com.smartflow.smestocksensebackend.service.WarehouseCapacityService warehouseCapacityService;

    @InjectMocks ReplenishmentSuggestionServiceImpl service;

    private java.math.BigDecimal unitVolumeM3;

    @BeforeEach
    void setUp() {
        unitVolumeM3 = java.math.BigDecimal.valueOf(0.1);
        lenient().when(warehouseCapacityService.getRemainingCapacity(any())).thenReturn(java.math.BigDecimal.valueOf(1000.0));
    }

    @Test void belowMinimum_shouldSuggestReplenishingToMinimum(){
        InventoryLevelProjection stock=stock(7,10,30);
        when(repository.findInventory(isNull(),isNull(),isNull(),eq("LOW_STOCK"),eq("HOAT_DONG"),eq("HOAT_DONG"),any()))
                .thenReturn(new PageImpl<>(List.of(stock)));
        ReplenishmentSuggestionResponse result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(3,result.shortageQuantity()); assertEquals(3,result.suggestedQuantity());
        assertEquals(ReplenishmentReason.BELOW_MINIMUM,result.reason()); assertEquals(ReplenishmentPriority.HIGH,result.priority());
        verify(repository,never()).save(any());
    }

    @Test void atMinimum_shouldSuggestZero(){
        InventoryLevelProjection stock=stock(10,10,30);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(0,result.shortageQuantity()); assertEquals(0,result.suggestedQuantity()); assertEquals(ReplenishmentReason.AT_MINIMUM,result.reason());
    }

    @Test void outOfStock_shouldBeCritical(){
        InventoryLevelProjection stock=stock(0,10,30);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(10,result.suggestedQuantity()); assertEquals(ReplenishmentPriority.CRITICAL,result.priority());
    }

    @Test void missingVolume_shouldWarn(){
        unitVolumeM3 = null;
        InventoryLevelProjection stock=stock(4,10,null);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(6,result.suggestedQuantity()); assertEquals("UNIT_VOLUME_NOT_CONFIGURED",result.configurationWarning());
    }

    @Test void capacityLimited_shouldReduceSuggestedQuantity() {
        unitVolumeM3 = java.math.BigDecimal.valueOf(1.0);
        when(warehouseCapacityService.getRemainingCapacity(any())).thenReturn(java.math.BigDecimal.valueOf(2.0));
        InventoryLevelProjection stock=stock(4,10,null);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(6,result.suggestedQuantity());
        assertEquals(2,result.capacityAllowedQuantity());
        assertTrue(result.capacityLimited());
        assertNotNull(result.configurationWarning());
    }

    @Test void shouldNormalizeKeywordAndDelegateAllFilters(){
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(Page.empty());
        service.listSuggestions(11L,44L," SP001 ",PageRequest.of(1,5));
        verify(repository).findInventory(eq(11L),eq(44L),eq("%SP001%"),eq("LOW_STOCK"),eq("HOAT_DONG"),eq("HOAT_DONG"),any());
    }

    private InventoryLevelProjection stock(Integer current,Integer min,Integer max){
        InventoryLevelProjection p=mock(InventoryLevelProjection.class); when(p.getProductId()).thenReturn(44L); when(p.getProductCode()).thenReturn("SP001");
        when(p.getProductName()).thenReturn("Laptop"); when(p.getWarehouseId()).thenReturn(11L); when(p.getWarehouseCode()).thenReturn("K001"); when(p.getWarehouse()).thenReturn("Kho chinh");
        when(p.getCurrentQuantity()).thenReturn(current); when(p.getMinStock()).thenReturn(min); when(p.getUnitVolumeM3()).thenReturn(unitVolumeM3); return p;
    }
}
