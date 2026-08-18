package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ForecastDriftLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastDriftLogRepository extends JpaRepository<ForecastDriftLog, Long> {

    List<ForecastDriftLog> findByProductIdAndWarehouseIdOrderByDetectedAtDesc(Long productId, Long warehouseId);
}
