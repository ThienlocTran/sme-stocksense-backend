package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.dto.inventory.DailyQuantityProjection;
import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>,
        JpaSpecificationExecutor<InventoryTransaction> {

    /**
     * Lấy danh sách giao dịch kho theo ID sản phẩm và ID kho.
     */
    List<InventoryTransaction> findByProductIdAndWarehouseIdOrderByCreatedAtDesc(Long productId, Long warehouseId);

    /**
     * Tìm với Specification nhưng eager load các association thường dùng để tránh
     * N+1.
     */
    @EntityGraph(attributePaths = { "product", "warehouse", "createdBy" })
    Page<InventoryTransaction> findAll(Specification<InventoryTransaction> spec, Pageable pageable);

    /**
     * Tổng số lượng xuất kho (XUAT_KHO) theo từng ngày trong khoảng [start, end] -
     * dùng để so sánh với dự báo AI đã lưu (drift detection).
     */
    @Query(value = "SELECT CAST(gd.ngay_tao AS date) AS ngay, SUM(gd.so_luong) AS tongSoLuong "
            + "FROM giao_dich_kho gd "
            + "WHERE gd.san_pham_id = :productId AND gd.kho_id = :warehouseId "
            + "AND gd.loai_giao_dich = 'XUAT_KHO' "
            + "AND CAST(gd.ngay_tao AS date) BETWEEN :start AND :end "
            + "GROUP BY CAST(gd.ngay_tao AS date) "
            + "ORDER BY ngay ASC", nativeQuery = true)
    List<DailyQuantityProjection> sumDailyXuatKho(@Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
