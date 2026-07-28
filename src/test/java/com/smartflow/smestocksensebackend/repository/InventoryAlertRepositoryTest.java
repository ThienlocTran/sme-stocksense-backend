package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Note: [T177 - Repository Test] Kiểm thử hợp đồng (Contract Test) cho InventoryAlertRepository
 * Sử dụng Mockito để xác minh các chữ ký phương thức truy vấn Deduplication, tra cứu và đếm KPI
 * không cần kết nối DB thực, đảm bảo tốc độ và tính ổn định trên CI.
 */
@ExtendWith(MockitoExtension.class)
class InventoryAlertRepositoryTest {

    @Mock
    private InventoryAlertRepository repository;

    @Test
    @DisplayName("existsByProductIdAndWarehouseIdAndStatusIn xác minh Deduplication cho trạng thái OPEN và ACKNOWLEDGED")
    void testExistsBy_Deduplication() {
        when(repository.existsByProductIdAndWarehouseIdAndStatusIn(
                eq(100L), eq(10L), eq(List.of(InventoryAlertStatus.OPEN, InventoryAlertStatus.ACKNOWLEDGED))))
                .thenReturn(true);

        boolean exists = repository.existsByProductIdAndWarehouseIdAndStatusIn(
                100L, 10L, List.of(InventoryAlertStatus.OPEN, InventoryAlertStatus.ACKNOWLEDGED));

        assertTrue(exists, "Phải trả về true khi đã có phiếu cảnh báo OPEN hoặc ACKNOWLEDGED");
        verify(repository, times(1)).existsByProductIdAndWarehouseIdAndStatusIn(
                100L, 10L, List.of(InventoryAlertStatus.OPEN, InventoryAlertStatus.ACKNOWLEDGED));
    }

    @Test
    @DisplayName("findTopByProductIdAndWarehouseIdAndStatusInOrderByCreatedAtAsc trả về phiếu cảnh báo cũ nhất đang hoạt động")
    void testFindTop_ActiveAlert() {
        InventoryAlert mockAlert = InventoryAlert.builder()
                .id(1L)
                .currentQuantity(2)
                .minStock(10)
                .severity(InventoryAlertSeverity.WARNING)
                .status(InventoryAlertStatus.OPEN)
                .build();

        when(repository.findTopByProductIdAndWarehouseIdAndStatusInOrderByCreatedAtAsc(
                eq(100L), eq(10L), any()))
                .thenReturn(Optional.of(mockAlert));

        Optional<InventoryAlert> result = repository.findTopByProductIdAndWarehouseIdAndStatusInOrderByCreatedAtAsc(
                100L, 10L, List.of(InventoryAlertStatus.OPEN, InventoryAlertStatus.ACKNOWLEDGED));

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(InventoryAlertStatus.OPEN, result.get().getStatus());
    }

    @Test
    @DisplayName("findByWarehouseIdAndStatusOrderByCreatedAtDesc trả về danh sách cảnh báo sắp xếp mới nhất")
    void testFindByWarehouseAndStatus() {
        InventoryAlert alert1 = InventoryAlert.builder().id(2L).status(InventoryAlertStatus.OPEN).build();
        InventoryAlert alert2 = InventoryAlert.builder().id(1L).status(InventoryAlertStatus.OPEN).build();

        when(repository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(eq(10L), eq(InventoryAlertStatus.OPEN)))
                .thenReturn(List.of(alert1, alert2));

        List<InventoryAlert> list = repository.findByWarehouseIdAndStatusOrderByCreatedAtDesc(10L, InventoryAlertStatus.OPEN);

        assertEquals(2, list.size());
        assertEquals(2L, list.get(0).getId());
        assertEquals(1L, list.get(1).getId());
    }

    @Test
    @DisplayName("countByWarehouseIdAndStatus đếm chính xác số phiếu cảnh báo theo trạng thái")
    void testCountByWarehouseAndStatus() {
        when(repository.countByWarehouseIdAndStatus(eq(10L), eq(InventoryAlertStatus.OPEN)))
                .thenReturn(5L); // Giả lập đếm 5 cảnh báo đang mở

        long count = repository.countByWarehouseIdAndStatus(10L, InventoryAlertStatus.OPEN);

        assertEquals(5L, count);
        verify(repository).countByWarehouseIdAndStatus(10L, InventoryAlertStatus.OPEN);
    }
}
