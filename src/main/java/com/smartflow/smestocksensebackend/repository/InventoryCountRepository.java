package com.smartflow.smestocksensebackend.repository;
import com.smartflow.smestocksensebackend.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;
public interface InventoryCountRepository extends JpaRepository<InventoryCount,Long> {
    boolean existsByWarehouseIdAndStatusIn(Long warehouseId, Collection<InventoryCountStatus> statuses);
    Page<InventoryCount> findByWarehouseId(Long warehouseId, Pageable pageable);
    Page<InventoryCount> findByStatus(InventoryCountStatus status, Pageable pageable);
    Page<InventoryCount> findByWarehouseIdAndStatus(Long warehouseId, InventoryCountStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InventoryCount c where c.id = :id")
    Optional<InventoryCount> findByIdForUpdate(@Param("id") Long id);
}
