package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.SalesHistory;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesHistoryRepository extends JpaRepository<SalesHistory, Long>, JpaSpecificationExecutor<SalesHistory> {

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse"})
    Page<SalesHistory> findAll(Specification<SalesHistory> spec, Pageable pageable);

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

    @Query(value = """
            select count(*) as rowCount,
                   count(distinct h.san_pham_id) as distinctProducts,
                   count(distinct h.kho_id) as distinctWarehouses,
                   count(distinct (h.san_pham_id, h.kho_id)) as distinctCombinations,
                   min(h.ngay) as minDate,
                   max(h.ngay) as maxDate
            from ai.lich_su_ban_hang h
            join san_pham p on p.id = h.san_pham_id
            join kho w on w.id = h.kho_id
            where (:source is null or h.nguon_du_lieu = :source)
              and (:product is null or lower(p.ma_san_pham) like :product or lower(p.ten_san_pham) like :product
                   or cast(p.id as text) = :productExact)
              and (:warehouse is null or lower(w.ma_kho) like :warehouse or lower(w.ten_kho) like :warehouse
                   or cast(w.id as text) = :warehouseExact)
              and (cast(:dateFrom as date) is null or h.ngay >= cast(:dateFrom as date))
              and (cast(:dateTo as date) is null or h.ngay <= cast(:dateTo as date))
            """, nativeQuery = true)
    SalesHistorySummaryRow summarize(
            @Param("source") String source,
            @Param("product") String product,
            @Param("productExact") String productExact,
            @Param("warehouse") String warehouse,
            @Param("warehouseExact") String warehouseExact,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);

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

    interface SalesHistorySummaryRow {
        Long getRowCount();
        Long getDistinctProducts();
        Long getDistinctWarehouses();
        Long getDistinctCombinations();
        LocalDate getMinDate();
        LocalDate getMaxDate();
    }
}
