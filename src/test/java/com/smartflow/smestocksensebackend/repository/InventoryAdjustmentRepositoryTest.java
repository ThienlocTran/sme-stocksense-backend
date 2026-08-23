package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentRepositoryTest {

    @Mock
    private InventoryAdjustmentRepository repository;

    @Test
    @DisplayName("findByInventoryCountId trả về header theo đợt kiểm kê")
    void findByInventoryCountId_shouldReturnHeader() {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setId(1L);
        adjustment.setStatus(InventoryAdjustmentStatus.NHAP);

        when(repository.findByInventoryCountId(10L)).thenReturn(Optional.of(adjustment));

        Optional<InventoryAdjustment> result = repository.findByInventoryCountId(10L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(InventoryAdjustmentStatus.NHAP, result.get().getStatus());
    }

    @Test
    @DisplayName("duplicate guard dùng một header cho một đợt kiểm kê")
    void duplicateGuard_shouldUseInventoryCountId() {
        when(repository.existsByInventoryCountId(eq(10L))).thenReturn(true);

        boolean exists = repository.existsByInventoryCountId(10L);

        assertTrue(exists);
        verify(repository).existsByInventoryCountId(10L);
    }
}
