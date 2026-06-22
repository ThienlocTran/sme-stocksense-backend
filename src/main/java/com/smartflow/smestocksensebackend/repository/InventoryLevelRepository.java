package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Long> {

    /**
     * Tìm bản ghi tồn kho theo ID sản phẩm và ID kho.
     * Do có ràng buộc unique (san_pham_id, kho_id), kết quả trả về tối đa là 1 bản ghi.
     *
     * @param productId ID sản phẩm
     * @param warehouseId ID kho hàng
     * @return Optional chứa thông tin tồn kho
     */
    Optional<InventoryLevel> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    /**
     * Tìm bản ghi tồn kho với Pessimistic Write Lock để tránh race condition.
     * Dùng trong các luồng cập nhật đồng thời (concurrent inventory increase).
     * Khóa bản ghi tại DB (SELECT ... FOR UPDATE) cho đến khi transaction kết thúc.
     *
     * @param productId ID sản phẩm
     * @param warehouseId ID kho hàng
     * @return Optional chứa thông tin tồn kho đang bị khóa
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryLevel i where i.product.id = :productId and i.warehouse.id = :warehouseId")
    Optional<InventoryLevel> findByProductIdAndWarehouseIdForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
}
