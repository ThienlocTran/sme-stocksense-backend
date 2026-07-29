package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.dto.replenishment.*;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
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
    @InjectMocks ReplenishmentSuggestionServiceImpl service;

    @Test void belowMinimum_shouldSuggestReplenishingToMaximum(){
        InventoryLevelProjection stock=stock(7,10,30);
        when(repository.findInventory(isNull(),isNull(),isNull(),eq("LOW_STOCK"),eq("HOAT_DONG"),eq("HOAT_DONG"),any()))
                .thenReturn(new PageImpl<>(List.of(stock)));
        ReplenishmentSuggestionResponse result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(3,result.shortageQuantity()); assertEquals(23,result.suggestedQuantity());
        assertEquals(ReplenishmentReason.BELOW_MINIMUM,result.reason()); assertEquals(ReplenishmentPriority.HIGH,result.priority());
        verify(repository,never()).save(any());
    }

    @Test void atMinimum_shouldStillSuggestToMaximum(){
        InventoryLevelProjection stock=stock(10,10,30);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(0,result.shortageQuantity()); assertEquals(20,result.suggestedQuantity()); assertEquals(ReplenishmentReason.AT_MINIMUM,result.reason());
    }

    @Test void outOfStock_shouldBeCritical(){
        InventoryLevelProjection stock=stock(0,10,30);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(30,result.suggestedQuantity()); assertEquals(ReplenishmentPriority.CRITICAL,result.priority());
    }

    @Test void missingMaximum_shouldOnlyCoverShortageAndWarn(){
        InventoryLevelProjection stock=stock(4,10,null);
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(new PageImpl<>(List.of(stock)));
        var result=service.listSuggestions(null,null,null,PageRequest.of(0,20)).getContent().getFirst();
        assertEquals(6,result.suggestedQuantity()); assertEquals("MAX_STOCK_NOT_CONFIGURED",result.configurationWarning());
    }

    @Test void shouldNormalizeKeywordAndDelegateAllFilters(){
        when(repository.findInventory(any(),any(),any(),any(),any(),any(),any())).thenReturn(Page.empty());
        service.listSuggestions(11L,44L," SP001 ",PageRequest.of(1,5));
        verify(repository).findInventory(eq(11L),eq(44L),eq("%SP001%"),eq("LOW_STOCK"),eq("HOAT_DONG"),eq("HOAT_DONG"),any());
    }

    private InventoryLevelProjection stock(Integer current,Integer min,Integer max){
        InventoryLevelProjection p=mock(InventoryLevelProjection.class); when(p.getProductId()).thenReturn(44L); when(p.getProductCode()).thenReturn("SP001");
        when(p.getProductName()).thenReturn("Laptop"); when(p.getWarehouseId()).thenReturn(11L); when(p.getWarehouseCode()).thenReturn("K001"); when(p.getWarehouse()).thenReturn("Kho chinh");
        when(p.getCurrentQuantity()).thenReturn(current); when(p.getMinStock()).thenReturn(min); when(p.getMaxStock()).thenReturn(max); return p;
    }
}
