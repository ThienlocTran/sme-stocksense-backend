package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryLevelRepository
        extends JpaRepository<InventoryLevel, Long>, JpaSpecificationExecutor<InventoryLevel> {

    // 1. Hàm đọc bình thường (Cho API xem danh sách tồn kho - không khóa)
    Optional<InventoryLevel> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    // 2. Hàm ĐỌC VÀ KHÓA CỨNG (Dành riêng cho Service T73 để cộng/trừ, chống Lost Update)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryLevel i WHERE i.product.id = :productId AND i.warehouse.id = :warehouseId")
    Optional<InventoryLevel> findByProductIdAndWarehouseIdForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
}