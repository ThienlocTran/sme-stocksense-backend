package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryAdjustmentLineRepository extends JpaRepository<InventoryAdjustmentLine, Long> {
    List<InventoryAdjustmentLine> findByAdjustmentIdOrderByIdAsc(Long adjustmentId);
}
