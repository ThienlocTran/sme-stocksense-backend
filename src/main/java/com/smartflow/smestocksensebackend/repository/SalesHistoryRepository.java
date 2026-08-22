package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.SalesHistory;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesHistoryRepository extends JpaRepository<SalesHistory, Long> {

    List<SalesHistory> findByProductIdAndWarehouseIdOrderByNgayAsc(Long productId, Long warehouseId);

    List<SalesHistory> findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(Long productId, Long warehouseId,
            SalesHistorySource source);

    long countByProductIdAndWarehouseId(Long productId, Long warehouseId);

    long countByProductIdAndWarehouseIdAndSource(Long productId, Long warehouseId, SalesHistorySource source);

    void deleteByProductIdAndWarehouseId(Long productId, Long warehouseId);

    Optional<SalesHistory> findByProductIdAndWarehouseIdAndNgay(Long productId, Long warehouseId, LocalDate ngay);

    Optional<SalesHistory> findByProductIdAndWarehouseIdAndNgayAndSource(Long productId, Long warehouseId,
            LocalDate ngay, SalesHistorySource source);

    @Query("""
            select p.id as productId, p.code as productCode, p.name as productName,
                   w.id as warehouseId, w.code as warehouseCode, w.name as warehouseName,
                   count(h.id) as historyDays, min(h.ngay) as historyStart, max(h.ngay) as historyEnd
            from SalesHistory h
            join h.product p
            join h.warehouse w
            where h.source = :source
            group by p.id, p.code, p.name, w.id, w.code, w.name
            having count(h.id) >= :minHistoryDays
            order by p.code asc, w.code asc
            """)
    List<ForecastAvailabilityRow> findForecastAvailability(SalesHistorySource source, long minHistoryDays);

    interface ForecastAvailabilityRow {
        Long getProductId();
        String getProductCode();
        String getProductName();
        Long getWarehouseId();
        String getWarehouseCode();
        String getWarehouseName();
        Long getHistoryDays();
        LocalDate getHistoryStart();
        LocalDate getHistoryEnd();
    }
}
