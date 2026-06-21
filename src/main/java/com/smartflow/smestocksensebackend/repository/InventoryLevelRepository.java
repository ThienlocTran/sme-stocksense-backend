package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryLevelRepository
        extends JpaRepository<InventoryLevel, Long>, JpaSpecificationExecutor<InventoryLevel> {

    @Query("SELECT t.id as inventoryId, sp.id as productId, sp.code as productCode, sp.name as productName, sp.barcode as barcode, " +
            "k.id as warehouseId, k.code as warehouseCode, k.name as warehouseName, t.quantity as quantity, sp.minStock as minStock, sp.maxStock as maxStock, " +
            "sp.status as productStatus, k.status as warehouseStatus, " +
            "CASE WHEN t.quantity <= 0 THEN 'OUT_OF_STOCK' WHEN t.quantity <= sp.minStock THEN 'LOW_STOCK' WHEN sp.maxStock IS NOT NULL AND t.quantity >= sp.maxStock THEN 'OVER_STOCK' ELSE 'NORMAL' END as stockStatus, t.updatedAt as lastUpdatedAt " +
            "FROM InventoryLevel t JOIN t.product sp JOIN t.warehouse k " +
            "WHERE (:warehouseId IS NULL OR k.id = :warehouseId) " +
            "AND (:productId IS NULL OR sp.id = :productId) " +
            "AND (:keyword IS NULL OR (LOWER(sp.code) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(sp.name) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(sp.barcode) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(k.code) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(k.name) LIKE LOWER(CONCAT('%',:keyword,'%')))) " +
            "AND (:stockStatus IS NULL OR (CASE WHEN t.quantity <= 0 THEN 'OUT_OF_STOCK' WHEN t.quantity <= sp.minStock THEN 'LOW_STOCK' WHEN sp.maxStock IS NOT NULL AND t.quantity >= sp.maxStock THEN 'OVER_STOCK' ELSE 'NORMAL' END) = :stockStatus)")
    Page<InventoryLevelProjection> findInventory(
            @Param("warehouseId") Long warehouseId,
            @Param("productId") Long productId,
            @Param("keyword") String keyword,
            @Param("stockStatus") String stockStatus,
            Pageable pageable);
}
