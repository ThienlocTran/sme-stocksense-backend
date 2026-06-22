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

        @Query(value = "SELECT t.id AS \"inventoryId\", sp.id AS \"productId\", sp.ma_san_pham AS \"productCode\", sp.ten_san_pham AS \"productName\", sp.ma_vach AS \"barcode\", "
                        +
                        "k.id AS \"warehouseId\", k.ma_kho AS \"warehouseCode\", k.ten_kho AS \"warehouse\", t.so_luong AS \"currentQuantity\", sp.ton_toi_thieu AS \"minStock\", sp.ton_toi_da AS \"maxStock\", "
                        +
                        "sp.trang_thai AS \"productStatus\", k.trang_thai AS \"warehouseStatus\", " +
                        "CASE WHEN t.so_luong = 0 THEN 'OUT_OF_STOCK' WHEN t.so_luong <= sp.ton_toi_thieu THEN 'LOW_STOCK' WHEN sp.ton_toi_da IS NOT NULL AND t.so_luong >= sp.ton_toi_da THEN 'OVER_STOCK' ELSE 'NORMAL' END AS \"status\", t.ngay_cap_nhat AS \"lastUpdatedAt\" "
                        +
                        "FROM ton_kho t " +
                        "JOIN san_pham sp ON sp.id = t.san_pham_id " +
                        "JOIN kho k ON k.id = t.kho_id " +
                        "WHERE (:warehouseId IS NULL OR k.id = :warehouseId) " +
                        "AND (:productId IS NULL OR sp.id = :productId) " +
                        "AND (:keyword IS NULL OR (sp.ma_san_pham::text ILIKE :keyword " +
                        "OR sp.ten_san_pham::text ILIKE :keyword " +
                        "OR sp.ma_vach::text ILIKE :keyword " +
                        "OR k.ma_kho::text ILIKE :keyword " +
                        "OR k.ten_kho::text ILIKE :keyword)) " +
                        "AND (:stockStatus IS NULL OR (CASE WHEN t.so_luong = 0 THEN 'OUT_OF_STOCK' WHEN t.so_luong <= sp.ton_toi_thieu THEN 'LOW_STOCK' WHEN sp.ton_toi_da IS NOT NULL AND t.so_luong >= sp.ton_toi_da THEN 'OVER_STOCK' ELSE 'NORMAL' END) = :stockStatus) "
                        +
                        "ORDER BY t.id DESC", countQuery = "SELECT COUNT(*) " +
                                        "FROM ton_kho t " +
                                        "JOIN san_pham sp ON sp.id = t.san_pham_id " +
                                        "JOIN kho k ON k.id = t.kho_id " +
                                        "WHERE (:warehouseId IS NULL OR k.id = :warehouseId) " +
                                        "AND (:productId IS NULL OR sp.id = :productId) " +
                                        "AND (:keyword IS NULL OR (sp.ma_san_pham::text ILIKE :keyword " +
                                        "OR sp.ten_san_pham::text ILIKE :keyword " +
                                        "OR sp.ma_vach::text ILIKE :keyword " +
                                        "OR k.ma_kho::text ILIKE :keyword " +
                                        "OR k.ten_kho::text ILIKE :keyword)) " +
                                        "AND (:stockStatus IS NULL OR (CASE WHEN t.so_luong = 0 THEN 'OUT_OF_STOCK' WHEN t.so_luong <= sp.ton_toi_thieu THEN 'LOW_STOCK' WHEN sp.ton_toi_da IS NOT NULL AND t.so_luong >= sp.ton_toi_da THEN 'OVER_STOCK' ELSE 'NORMAL' END) = :stockStatus)", nativeQuery = true)
        Page<InventoryLevelProjection> findInventory(
                        @Param("warehouseId") Long warehouseId,
                        @Param("productId") Long productId,
                        @Param("keyword") String keyword,
                        @Param("stockStatus") String stockStatus,
                        Pageable pageable);
}
