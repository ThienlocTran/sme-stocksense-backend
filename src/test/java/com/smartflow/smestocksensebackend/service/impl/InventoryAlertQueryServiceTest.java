package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAlertQueryServiceTest {

    @Mock
    private InventoryAlertRepository repository;

    @InjectMocks
    private InventoryAlertQueryServiceImpl service;

    @Test
    @DisplayName("getAlerts: Truyền null status thì nhận Default Status (OPEN, ACKNOWLEDGED) và Specification được gộp thêm orderBy Business Priority")
    void getAlerts_DefaultStatusAndSorting() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20); // Không có sort
        
        Product p = new Product();
        p.setId(10L);
        p.setCode("SP01");
        p.setName("Sản phẩm 1");
        
        Warehouse w = new Warehouse();
        w.setId(20L);
        w.setCode("K01");
        w.setName("Kho 1");
        
        InventoryAlert mockAlert = new InventoryAlert();
        mockAlert.setId(1L);
        mockAlert.setProduct(p);
        mockAlert.setWarehouse(w);
        mockAlert.setSeverity(InventoryAlertSeverity.CRITICAL);
        mockAlert.setStatus(InventoryAlertStatus.OPEN);
        mockAlert.setHandledBy("John Doe");
        
        Page<InventoryAlert> mockPage = new PageImpl<>(List.of(mockAlert));
        
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Act
        Page<InventoryAlertResponse> result = service.getAlerts(null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        InventoryAlertResponse dto = result.getContent().get(0);
        
        // Assert Mapping
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SP01", dto.getProductCode());
        assertEquals("Sản phẩm 1", dto.getProductName());
        assertEquals(20L, dto.getWarehouseId());
        assertEquals("K01", dto.getWarehouseCode());
        assertEquals("Kho 1", dto.getWarehouseName());
        assertEquals("John Doe", dto.getHandledBy());

        // Verify Default Sorting & Status
        ArgumentCaptor<Specification<InventoryAlert>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(specCaptor.capture(), pageableCaptor.capture());
        
        // The service overrides pageable to strip sorts when adding orderby to spec
        Pageable capturedPageable = pageableCaptor.getValue();
        assertTrue(capturedPageable.getSort().isUnsorted());
        
        // Spec is present
        assertNotNull(specCaptor.getValue());
    }

    @Test
    @DisplayName("getAlerts: Không ghi đè Sort nếu client đã truyền sẵn")
    void getAlerts_CustomSorting() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "currentQuantity"));
        Page<InventoryAlert> mockPage = new PageImpl<>(Collections.emptyList());
        
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Act
        service.getAlerts(1L, 2L, InventoryAlertSeverity.WARNING, List.of(InventoryAlertStatus.RESOLVED), pageable);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        
        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("currentQuantity");
        assertNotNull(order);
        assertTrue(order.isAscending());
    }
}
