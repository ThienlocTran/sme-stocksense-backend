package com.smartflow.smestocksensebackend.event;

import com.smartflow.smestocksensebackend.service.InventoryAlertDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit Test cho InventoryAlertEventListener (T184).
 * Kiểm tra Listener chỉ delegate, không chứa business logic.
 */
@ExtendWith(MockitoExtension.class)
class InventoryAlertEventListenerTest {

    @Mock
    private InventoryAlertDetectionService inventoryAlertDetectionService;

    @InjectMocks
    private InventoryAlertEventListener listener;

    // --- Listener chỉ delegate: verify gọi đúng service ---
    @Test
    void onInventoryLevelChanged_ShouldDelegateToDetectionService() {
        // Given
        InventoryLevelChangedEvent event = new InventoryLevelChangedEvent(1L, 2L, 20, 5, 10);

        // When
        listener.onInventoryLevelChanged(event);

        // Then: Listener phải uỷ quyền đúng 1 lần cho Detection Service
        verify(inventoryAlertDetectionService, times(1)).processInventoryChange(event);
    }

    // --- Listener catch exception: không ném ra ngoài khi service lỗi ---
    @Test
    void onInventoryLevelChanged_WhenServiceThrowsException_ShouldNotPropagateException() {
        // Given: Detection Service ném RuntimeException
        InventoryLevelChangedEvent event = new InventoryLevelChangedEvent(1L, 2L, 20, 5, 10);
        doThrow(new RuntimeException("DB connection failed"))
                .when(inventoryAlertDetectionService).processInventoryChange(event);

        // When / Then: Listener phải catch exception, không ném ra ngoài (bảo vệ Async thread)
        // Nếu listener ném ra thì test này sẽ fail
        listener.onInventoryLevelChanged(event);

        // Verify service đã được gọi (không phải bị skip)
        verify(inventoryAlertDetectionService, times(1)).processInventoryChange(event);
    }
}
