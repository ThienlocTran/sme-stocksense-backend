package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho thực thể InventoryTransaction (bảng "giao_dich_kho").
 * Kế thừa JpaSpecificationExecutor để hỗ trợ truy vấn lịch sử giao dịch động
 * (theo sản phẩm, kho, loại giao dịch, khoảng ngày...) phục vụ T64.
 */
@Repository
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long>, JpaSpecificationExecutor<InventoryTransaction> {

    /**
     * Lấy lịch sử giao dịch của 1 sản phẩm trong 1 kho, mới nhất trước.
     *
     * @param productId   id sản phẩm
     * @param warehouseId id kho
     * @return danh sách giao dịch sắp xếp theo ngày tạo giảm dần
     */
    List<InventoryTransaction> findByProductIdAndWarehouseIdOrderByCreatedAtDesc(Long productId, Long warehouseId);
}
