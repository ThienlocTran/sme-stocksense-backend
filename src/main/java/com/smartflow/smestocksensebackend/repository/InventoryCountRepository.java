package com.smartflow.smestocksensebackend.repository;
import com.smartflow.smestocksensebackend.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
public interface InventoryCountRepository extends JpaRepository<InventoryCount,Long> {
    boolean existsByWarehouseIdAndStatusIn(Long warehouseId, Collection<InventoryCountStatus> statuses);
    Page<InventoryCount> findByWarehouseId(Long warehouseId, Pageable pageable);
    Page<InventoryCount> findByStatus(InventoryCountStatus status, Pageable pageable);
    Page<InventoryCount> findByWarehouseIdAndStatus(Long warehouseId, InventoryCountStatus status, Pageable pageable);
}
