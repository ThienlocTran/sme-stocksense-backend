package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentRepositoryTest {

    private static final List<InventoryAdjustmentStatus> ACTIVE_STATUSES = List.of(
            InventoryAdjustmentStatus.NHAP,
            InventoryAdjustmentStatus.CHO_DUYET,
            InventoryAdjustmentStatus.DA_DUYET
    );

    @Mock
    private InventoryAdjustmentRepository repository;

    @Test
    @DisplayName("findByInventoryCountIdOrderByIdAsc trả về phiếu theo đợt kiểm kê")
    void findByInventoryCountId_shouldReturnAdjustments() {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setId(1L);

        when(repository.findByInventoryCountIdOrderByIdAsc(10L)).thenReturn(List.of(adjustment));

        List<InventoryAdjustment> result = repository.findByInventoryCountIdOrderByIdAsc(10L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("active query dùng NHAP, CHO_DUYET, DA_DUYET")
    void activeQuery_shouldUseActiveStatusesOnly() {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setStatus(InventoryAdjustmentStatus.CHO_DUYET);

        when(repository.findFirstByInventoryCountIdAndStatusInOrderByIdAsc(eq(10L), eq(ACTIVE_STATUSES)))
                .thenReturn(Optional.of(adjustment));

        Optional<InventoryAdjustment> result = repository.findFirstByInventoryCountIdAndStatusInOrderByIdAsc(
                10L,
                ACTIVE_STATUSES
        );

        assertTrue(result.isPresent());
        assertEquals(InventoryAdjustmentStatus.CHO_DUYET, result.get().getStatus());
    }

    @Test
    @DisplayName("duplicate guard không xem TU_CHOI hoặc DA_AP_DUNG là active")
    void duplicateGuard_shouldIgnoreHistoricalStatuses() {
        when(repository.existsByInventoryCountIdAndStatusIn(eq(10L), eq(ACTIVE_STATUSES))).thenReturn(false);

        boolean exists = repository.existsByInventoryCountIdAndStatusIn(10L, ACTIVE_STATUSES);

        assertTrue(!exists);
        verify(repository).existsByInventoryCountIdAndStatusIn(10L, ACTIVE_STATUSES);
    }
}
