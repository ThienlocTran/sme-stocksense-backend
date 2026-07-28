package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAlertQueryServiceImplTest {

    @Mock
    private InventoryAlertRepository inventoryAlertRepository;

    @InjectMocks
    private InventoryAlertQueryServiceImpl service;

    @Test
    void getAlerts_WithFilterFound_ShouldReturnMappedDto() {
        // Given
        Long warehouseId = 1L;
        InventoryAlertSeverity severity = InventoryAlertSeverity.CRITICAL;
        Pageable pageable = PageRequest.of(0, 10);
        
        InventoryAlert alert = new InventoryAlert();
        alert.setId(100L);
        alert.setStatus(InventoryAlertStatus.OPEN);
        alert.setSeverity(InventoryAlertSeverity.CRITICAL);
        alert.setCurrentQuantity(0);
        alert.setCreatedAt(LocalDateTime.now());
        
        Product product = new Product();
        product.setId(10L);
        product.setName("Test Product");
        alert.setProduct(product);
        
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Test Warehouse");
        alert.setWarehouse(warehouse);

        Page<InventoryAlert> page = new PageImpl<>(List.of(alert), pageable, 1);
        
        when(inventoryAlertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<InventoryAlertResponse> result = service.getAlerts(warehouseId, null, severity, null, pageable);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getId());
        assertEquals(InventoryAlertSeverity.CRITICAL, result.getContent().get(0).getSeverity());
    }

    @Test
    void getAlerts_WithoutFilterNotFound_ShouldReturnEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<InventoryAlert> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(inventoryAlertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<InventoryAlertResponse> result = service.getAlerts(999L, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }
    
    @Test
    void getAlerts_WithPagination_ShouldReturnPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(1, 2); // page 1, size 2
        
        InventoryAlert alert1 = new InventoryAlert();
        alert1.setId(101L);
        alert1.setProduct(new Product());
        alert1.setWarehouse(new Warehouse());

        InventoryAlert alert2 = new InventoryAlert();
        alert2.setId(102L);
        alert2.setProduct(new Product());
        alert2.setWarehouse(new Warehouse());
        
        // Total 5 elements, current page has 2 elements
        Page<InventoryAlert> page = new PageImpl<>(List.of(alert1, alert2), pageable, 5);
        
        when(inventoryAlertRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<InventoryAlertResponse> result = service.getAlerts(null, null, null, null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages()); // 5/2 = 3 pages
        assertEquals(101L, result.getContent().get(0).getId());
        assertEquals(102L, result.getContent().get(1).getId());
    }
}
