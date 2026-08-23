package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {

    List<ForecastResult> findByProductIdAndWarehouseIdAndVersion(Long productId, Long warehouseId, Integer version);

    List<ForecastResult> findByModelMetadataId(Long modelMetadataId);

    @Query("SELECT MAX(f.version) FROM ForecastResult f WHERE f.product.id = :productId AND f.warehouse.id = :warehouseId")
    Integer findMaxVersion(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    List<ForecastResult> findByProductIdAndWarehouseIdAndHorizonDaysAndForecastDateBetweenOrderByForecastDateAsc(
            Long productId, Long warehouseId, Integer horizonDays, LocalDate start, LocalDate end);
}
