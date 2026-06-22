package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
