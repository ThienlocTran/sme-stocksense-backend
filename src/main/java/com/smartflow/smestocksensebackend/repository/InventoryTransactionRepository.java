package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
