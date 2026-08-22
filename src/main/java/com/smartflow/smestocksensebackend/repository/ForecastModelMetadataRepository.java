package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ForecastModelMetadataRepository extends JpaRepository<ForecastModelMetadata, Long> {

    Optional<ForecastModelMetadata> findFirstByProductIdAndWarehouseIdOrderByVersionDesc(Long productId, Long warehouseId);

    Optional<ForecastModelMetadata> findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
            Long productId, Long warehouseId, SalesHistorySource historySource);
}
