package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.dto.dashboard.StockHealthProjection;
import com.smartflow.smestocksensebackend.dto.dashboard.WarehouseDistributionProjection;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {

        /**
         * Tìm bản ghi tồn kho theo ID sản phẩm và ID kho.
         * Do có ràng buộc unique (san_pham_id, kho_id), kết quả trả về tối đa là 1 bản
         * ghi.
         *
         * @param productId   ID sản phẩm
         * @param warehouseId ID kho hàng
         * @return Optional chứa thông tin tồn kho
         */
        Optional<InventoryLevel> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

        List<InventoryLevel> findByWarehouseId(Long warehouseId);

        @Query(value = "SELECT COALESCE(SUM(COALESCE(sp.the_tich_don_vi_m3, 0) * COALESCE(t.so_luong, 0)), 0) "
                        + "FROM ton_kho t "
                        + "JOIN san_pham sp ON sp.id = t.san_pham_id "
                        + "WHERE t.kho_id = :warehouseId", nativeQuery = true)
        java.math.BigDecimal sumUsedCapacityByWarehouseId(@Param("warehouseId") Long warehouseId);

        /**
         * Tìm bản ghi tồn kho với Pessimistic Write Lock để tránh race condition.
         * Dùng trong các luồng cập nhật đồng thời (concurrent inventory increase).
         * Khóa bản ghi tại DB (SELECT ... FOR UPDATE) cho đến khi transaction kết thúc.
         *
         * @param productId   ID sản phẩm
         * @param warehouseId ID kho hàng
         * @return Optional chứa thông tin tồn kho đang bị khóa
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select i from InventoryLevel i where i.product.id = :productId and i.warehouse.id = :warehouseId")
        Optional<InventoryLevel> findByProductIdAndWarehouseIdForUpdate(@Param("productId") Long productId,
                        @Param("warehouseId") Long warehouseId);

        @Query("SELECT v FROM InventoryLevel v JOIN v.warehouse w WHERE w.id = :warehouseId AND w.status = 'HOAT_DONG'")
        List<InventoryLevel> findActiveInventoryByWarehouse(@Param("warehouseId") Long warehouseId);

        @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryLevel i")
        long sumTotalQuantity();

        @Query("select i from InventoryLevel i where i.warehouse.id = :warehouseId and i.product.id in :productIds")
        List<InventoryLevel> findByWarehouseIdAndProductIdIn(@Param("warehouseId") Long warehouseId,
                        @Param("productIds") List<Long> productIds);

        /**
         * Chuẩn hóa Rule cảnh báo tồn kho thấp (Single Source of Truth tại DB SQL).
         * 1. Luồng đánh giá trạng thái (Evaluation Order):
         * - OUT_OF_STOCK: t.so_luong <= 0 (khẩn cấp Critical, bao gồm tồn kho = 0 và âm
         * do chênh lệch kiểm kê).
         * - LOW_STOCK: sp.ton_toi_thieu IS NOT NULL AND sp.ton_toi_thieu > 0 AND
         * t.so_luong <= sp.ton_toi_thieu (cảnh báo Warning).
         * - OVER_STOCK: sp.ton_toi_da IS NOT NULL AND sp.ton_toi_da > 0 AND t.so_luong
         * > sp.ton_toi_da (vượt định mức).
         * - NORMAL: Các trường hợp còn lại nằm trong ngưỡng an toàn.
         * 2. Phạm vi cảnh báo (/low-stock):
         * - Khi tham số :stockStatus = 'LOW_STOCK', query sẽ lọc cả 2 nhóm
         * ('LOW_STOCK', 'OUT_OF_STOCK')
         * để quản lý kho không bỏ sót mặt hàng đã hết hẳn theo Rule 2 trong Spec.
         */
        @Query(value = "SELECT t.id AS \"inventoryId\", sp.id AS \"productId\", sp.ma_san_pham AS \"productCode\", sp.ten_san_pham AS \"productName\", sp.ma_vach AS \"barcode\", "
                        +
                        "k.id AS \"warehouseId\", k.ma_kho AS \"warehouseCode\", k.ten_kho AS \"warehouse\", t.so_luong AS \"currentQuantity\", cc.min_stock AS \"minStock\", NULL AS \"maxStock\", "
                        +
                        "sp.trang_thai AS \"productStatus\", k.trang_thai AS \"warehouseStatus\", " +
                        "CASE WHEN t.so_luong <= 0 THEN 'OUT_OF_STOCK' WHEN cc.min_stock IS NOT NULL AND cc.min_stock > 0 AND t.so_luong <= cc.min_stock THEN 'LOW_STOCK' ELSE 'NORMAL' END AS \"status\", t.ngay_cap_nhat AS \"lastUpdatedAt\" "
                        +
                        "FROM ton_kho t " +
                        "JOIN san_pham sp ON sp.id = t.san_pham_id " +
                        "JOIN kho k ON k.id = t.kho_id " +
                        "LEFT JOIN cau_hinh_ton_kho cc ON cc.san_pham_id = t.san_pham_id AND cc.kho_id = t.kho_id " +
                        "WHERE (:warehouseId IS NULL OR k.id = :warehouseId) " +
                        "AND (:productId IS NULL OR sp.id = :productId) " +
                        "AND (:keyword IS NULL OR (sp.ma_san_pham::text ILIKE :keyword " +
                        "OR sp.ten_san_pham::text ILIKE :keyword " +
                        "OR sp.ma_vach::text ILIKE :keyword " +
                        "OR k.ma_kho::text ILIKE :keyword " +
                        "OR k.ten_kho::text ILIKE :keyword)) " +
                        "AND (:stockStatus IS NULL OR (:stockStatus = 'LOW_STOCK' AND (CASE WHEN t.so_luong <= 0 THEN 'OUT_OF_STOCK' WHEN cc.min_stock IS NOT NULL AND cc.min_stock > 0 AND t.so_luong <= cc.min_stock THEN 'LOW_STOCK' ELSE 'NORMAL' END) IN ('LOW_STOCK', 'OUT_OF_STOCK')) OR (:stockStatus != 'LOW_STOCK' AND (CASE WHEN t.so_luong <= 0 THEN 'OUT_OF_STOCK' WHEN cc.min_stock IS NOT NULL AND cc.min_stock > 0 AND t.so_luong <= cc.min_stock THEN 'LOW_STOCK' ELSE 'NORMAL' END) = :stockStatus)) "
                        +
                        "AND (:warehouseStatus IS NULL OR k.trang_thai::text = :warehouseStatus) " +
                        "AND (:productStatus IS NULL OR sp.trang_thai::text = :productStatus) " +
                        "ORDER BY t.id DESC", countQuery = "SELECT COUNT(*) " +
                                        "FROM ton_kho t " +
                                        "JOIN san_pham sp ON sp.id = t.san_pham_id " +
                                        "JOIN kho k ON k.id = t.kho_id " +
                                        "LEFT JOIN cau_hinh_ton_kho cc ON cc.san_pham_id = t.san_pham_id AND cc.kho_id = t.kho_id " +
                                        "WHERE (:warehouseId IS NULL OR k.id = :warehouseId) " +
                                        "AND (:productId IS NULL OR sp.id = :productId) " +
                                        "AND (:keyword IS NULL OR (sp.ma_san_pham::text ILIKE :keyword " +
                                        "OR sp.ten_san_pham::text ILIKE :keyword " +
                                        "OR sp.ma_vach::text ILIKE :keyword " +
                                        "OR k.ma_kho::text ILIKE :keyword " +
                                        "OR k.ten_kho::text ILIKE :keyword)) " +
                                        "AND (:stockStatus IS NULL OR (:stockStatus = 'LOW_STOCK' AND (CASE WHEN t.so_luong <= 0 THEN 'OUT_OF_STOCK' WHEN cc.min_stock IS NOT NULL AND cc.min_stock > 0 AND t.so_luong <= cc.min_stock THEN 'LOW_STOCK' ELSE 'NORMAL' END) IN ('LOW_STOCK', 'OUT_OF_STOCK')) OR (:stockStatus != 'LOW_STOCK' AND (CASE WHEN t.so_luong <= 0 THEN 'OUT_OF_STOCK' WHEN cc.min_stock IS NOT NULL AND cc.min_stock > 0 AND t.so_luong <= cc.min_stock THEN 'LOW_STOCK' ELSE 'NORMAL' END) = :stockStatus)) "
                                        +
                                        "AND (:warehouseStatus IS NULL OR k.trang_thai::text = :warehouseStatus) " +
                                        "AND (:productStatus IS NULL OR sp.trang_thai::text = :productStatus)", nativeQuery = true)
        Page<InventoryLevelProjection> findInventory(
                        @Param("warehouseId") Long warehouseId,
                        @Param("productId") Long productId,
                        @Param("keyword") String keyword,
                        @Param("stockStatus") String stockStatus,
                        @Param("warehouseStatus") String warehouseStatus,
                        @Param("productStatus") String productStatus,
                        Pageable pageable);

        @Query(value = "SELECT "
                        + "COALESCE(SUM(CASE WHEN t.so_luong > COALESCE(cc.min_stock, 0) THEN 1 ELSE 0 END), 0) AS \"healthy\", "
                        + "COALESCE(SUM(CASE WHEN t.so_luong > 0 AND cc.min_stock IS NOT NULL AND cc.min_stock > 0 AND t.so_luong <= cc.min_stock THEN 1 ELSE 0 END), 0) AS \"lowStock\", "
                        + "COALESCE(SUM(CASE WHEN t.so_luong <= 0 THEN 1 ELSE 0 END), 0) AS \"outOfStock\" "
                        + "FROM ton_kho t "
                        + "JOIN san_pham sp ON sp.id = t.san_pham_id "
                        + "JOIN kho k ON k.id = t.kho_id "
                        + "LEFT JOIN cau_hinh_ton_kho cc ON cc.san_pham_id = t.san_pham_id AND cc.kho_id = t.kho_id "
                        + "WHERE sp.trang_thai = 'HOAT_DONG' AND k.trang_thai = 'HOAT_DONG'", nativeQuery = true)
        StockHealthProjection countDashboardStockHealth();

        @Query(value = "SELECT k.id AS \"warehouseId\", k.ten_kho AS \"warehouseName\", COALESCE(SUM(t.so_luong), 0) AS \"totalQuantity\" "
                        + "FROM ton_kho t "
                        + "JOIN kho k ON k.id = t.kho_id "
                        + "JOIN san_pham sp ON sp.id = t.san_pham_id "
                        + "WHERE k.trang_thai = 'HOAT_DONG' AND sp.trang_thai = 'HOAT_DONG' "
                        + "GROUP BY k.id, k.ten_kho "
                        + "ORDER BY \"totalQuantity\" DESC, k.id ASC", nativeQuery = true)
        List<WarehouseDistributionProjection> sumDashboardWarehouseDistribution();
}
