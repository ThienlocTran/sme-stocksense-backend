package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ForecastDriftLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastDriftLogRepository extends JpaRepository<ForecastDriftLog, Long>,
        JpaSpecificationExecutor<ForecastDriftLog> {

    @Override
    @EntityGraph(attributePaths = {"product", "warehouse", "modelMetadata"})
    Page<ForecastDriftLog> findAll(Specification<ForecastDriftLog> spec, Pageable pageable);

    List<ForecastDriftLog> findByProductIdAndWarehouseIdOrderByDetectedAtDesc(Long productId, Long warehouseId);
}
