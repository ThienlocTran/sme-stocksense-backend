package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.exception.InvalidAlertStateException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAlertActionServiceImplTest {

    @Mock
    private InventoryAlertRepository inventoryAlertRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InventoryAlertActionServiceImpl inventoryAlertActionService;

    @BeforeEach
    void setUp() {
        // Setup Security Context
        SecurityContextHolder.setContext(securityContext);
    }
    
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acknowledgeAlert_WhenOpen_ShouldUpdateStatusAndSave() {
        // Given
        Long alertId = 1L;
        InventoryAlert alert = new InventoryAlert();
        alert.setId(alertId);
        alert.setStatus(InventoryAlertStatus.OPEN);
        alert.setHandledBy(null);

        when(inventoryAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");

        // When
        InventoryAlertResponse response = inventoryAlertActionService.acknowledgeAlert(alertId);

        // Then
        assertNotNull(response);
        assertEquals(InventoryAlertStatus.ACKNOWLEDGED, response.getStatus());
        assertEquals("testuser", response.getHandledBy());
        
        verify(inventoryAlertRepository).findById(alertId);
        // Không verify save() - Service dùng JPA dirty checking, Hibernate tự flush khi commit
    }

    @Test
    void acknowledgeAlert_WhenAlreadyAcknowledged_ShouldBeIdempotentAndNotSave() {
        // Given
        Long alertId = 1L;
        InventoryAlert alert = new InventoryAlert();
        alert.setId(alertId);
        alert.setStatus(InventoryAlertStatus.ACKNOWLEDGED);
        alert.setHandledBy("otheruser");

        when(inventoryAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        // When
        InventoryAlertResponse response = inventoryAlertActionService.acknowledgeAlert(alertId);

        // Then: Idempotent - handledBy giữ nguyên, không ghi đè, không gọi save
        assertNotNull(response);
        assertEquals(InventoryAlertStatus.ACKNOWLEDGED, response.getStatus());
        assertEquals("otheruser", response.getHandledBy());
        
        verify(inventoryAlertRepository).findById(alertId);
        verify(inventoryAlertRepository, never()).save(any());
    }

    @Test
    void acknowledgeAlert_WhenResolved_ShouldThrowExceptionAndNotSave() {
        // Given
        Long alertId = 1L;
        InventoryAlert alert = new InventoryAlert();
        alert.setId(alertId);
        alert.setStatus(InventoryAlertStatus.RESOLVED);

        when(inventoryAlertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        // When & Then
        assertThrows(InvalidAlertStateException.class, () -> 
            inventoryAlertActionService.acknowledgeAlert(alertId)
        );
        
        verify(inventoryAlertRepository).findById(alertId);
        verify(inventoryAlertRepository, never()).save(any());
    }

    @Test
    void acknowledgeAlert_WhenNotFound_ShouldThrowNotFoundException() {
        // Given
        Long alertId = 1L;
        when(inventoryAlertRepository.findById(alertId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> 
            inventoryAlertActionService.acknowledgeAlert(alertId)
        );
        
        verify(inventoryAlertRepository).findById(alertId);
        verify(inventoryAlertRepository, never()).save(any());
    }
}
