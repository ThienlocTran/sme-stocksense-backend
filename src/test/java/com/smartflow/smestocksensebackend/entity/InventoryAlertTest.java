package com.smartflow.smestocksensebackend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Note: [T177 - Unit Test] Kiểm thử cơ chế Transition Guard (canAcknowledge / canResolve)
 * và luồng nghiệp vụ chuyển đổi trạng thái (acknowledge / resolve) của Entity InventoryAlert.
 */
class InventoryAlertTest {

    @Test
    @DisplayName("canAcknowledge trả về true khi ở trạng thái OPEN, false khi đã RESOLVED hoặc ACKNOWLEDGED")
    void testCanAcknowledge() {
        InventoryAlert alertOpen = InventoryAlert.builder().status(InventoryAlertStatus.OPEN).build();
        assertTrue(alertOpen.canAcknowledge());

        InventoryAlert alertAck = InventoryAlert.builder().status(InventoryAlertStatus.ACKNOWLEDGED).build();
        assertFalse(alertAck.canAcknowledge());

        InventoryAlert alertResolved = InventoryAlert.builder().status(InventoryAlertStatus.RESOLVED).build();
        assertFalse(alertResolved.canAcknowledge());
    }

    @Test
    @DisplayName("canResolve trả về true khi ở trạng thái OPEN hoặc ACKNOWLEDGED, false khi đã RESOLVED")
    void testCanResolve() {
        InventoryAlert alertOpen = InventoryAlert.builder().status(InventoryAlertStatus.OPEN).build();
        assertTrue(alertOpen.canResolve());

        InventoryAlert alertAck = InventoryAlert.builder().status(InventoryAlertStatus.ACKNOWLEDGED).build();
        assertTrue(alertAck.canResolve());

        InventoryAlert alertResolved = InventoryAlert.builder().status(InventoryAlertStatus.RESOLVED).build();
        assertFalse(alertResolved.canResolve());
    }

    @Test
    @DisplayName("acknowledge chuyển trạng thái thành công từ OPEN sang ACKNOWLEDGED và ghi nhận actor/note")
    void testAcknowledge_Success() {
        InventoryAlert alert = InventoryAlert.builder()
                .status(InventoryAlertStatus.OPEN)
                .build();

        alert.acknowledge("user_hcm", "Đang đặt mua 500 cái");

        assertEquals(InventoryAlertStatus.ACKNOWLEDGED, alert.getStatus());
        assertEquals("user_hcm", alert.getHandledBy());
        assertEquals("Đang đặt mua 500 cái", alert.getNote());
    }

    @Test
    @DisplayName("acknowledge ném IllegalStateException khi cố xác nhận trên cảnh báo đã RESOLVED")
    void testAcknowledge_WhenResolved_ThrowsException() {
        InventoryAlert alert = InventoryAlert.builder()
                .status(InventoryAlertStatus.RESOLVED)
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> alert.acknowledge("admin", "test"));
        assertTrue(ex.getMessage().contains("OPEN"));
    }

    @Test
    @DisplayName("resolve từ OPEN hoặc ACKNOWLEDGED thành công và ghi nhận resolvedAt")
    void testResolve_Success() {
        InventoryAlert alert = InventoryAlert.builder()
                .status(InventoryAlertStatus.ACKNOWLEDGED)
                .handledBy("user_hcm")
                .build();

        alert.resolve("SYSTEM");

        assertEquals(InventoryAlertStatus.RESOLVED, alert.getStatus());
        assertEquals("SYSTEM", alert.getHandledBy());
        assertNotNull(alert.getResolvedAt());
    }

    @Test
    @DisplayName("resolve trên cảnh báo đã RESOLVED có tính idempotent, không lỗi và không thay đổi dữ liệu")
    void testResolve_WhenAlreadyResolved_IsIdempotent() {
        InventoryAlert alert = InventoryAlert.builder()
                .status(InventoryAlertStatus.RESOLVED)
                .handledBy("admin")
                .build();

        alert.resolve("user2");

        assertEquals(InventoryAlertStatus.RESOLVED, alert.getStatus());
        assertEquals("admin", alert.getHandledBy()); // giữ nguyên người giải quyết đầu tiên
    }
}
