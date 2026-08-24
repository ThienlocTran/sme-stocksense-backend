package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    Optional<InventoryAdjustment> findByInventoryCountId(Long inventoryCountId);
    boolean existsByInventoryCountId(Long inventoryCountId);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from InventoryAdjustment a where a.id = :id")
    Optional<InventoryAdjustment> findByIdForUpdate(@Param("id") Long id);
}
