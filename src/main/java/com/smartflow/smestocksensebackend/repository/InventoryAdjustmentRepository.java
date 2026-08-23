package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findByInventoryCountIdOrderByIdAsc(Long inventoryCountId);
    Optional<InventoryAdjustment> findFirstByInventoryCountIdAndStatusInOrderByIdAsc(
            Long inventoryCountId,
            Collection<InventoryAdjustmentStatus> statuses
    );
    boolean existsByInventoryCountIdAndStatusIn(Long inventoryCountId, Collection<InventoryAdjustmentStatus> statuses);
}
