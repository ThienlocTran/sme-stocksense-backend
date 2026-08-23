package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    Optional<InventoryAdjustment> findByInventoryCountId(Long inventoryCountId);
    boolean existsByInventoryCountId(Long inventoryCountId);
    boolean existsByCodeIgnoreCase(String code);
}
