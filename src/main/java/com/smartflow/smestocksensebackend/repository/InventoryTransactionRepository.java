package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    /**
     * Lấy danh sách giao dịch kho theo ID sản phẩm và ID kho.
     *
     * @param productId ID sản phẩm
     * @param warehouseId ID kho hàng
     * @return Danh sách giao dịch kho
     */
    List<InventoryTransaction> findByProductIdAndWarehouseIdOrderByCreatedAtDesc(Long productId, Long warehouseId);
}
