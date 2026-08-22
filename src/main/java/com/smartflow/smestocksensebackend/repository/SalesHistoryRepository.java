package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.SalesHistory;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesHistoryRepository extends JpaRepository<SalesHistory, Long> {

    List<SalesHistory> findByProductIdAndWarehouseIdOrderByNgayAsc(Long productId, Long warehouseId);

    long countByProductIdAndWarehouseId(Long productId, Long warehouseId);

    void deleteByProductIdAndWarehouseId(Long productId, Long warehouseId);

    Optional<SalesHistory> findByProductIdAndWarehouseIdAndNgay(Long productId, Long warehouseId, LocalDate ngay);

    Optional<SalesHistory> findByProductIdAndWarehouseIdAndNgayAndSource(Long productId, Long warehouseId,
            LocalDate ngay, SalesHistorySource source);
}
