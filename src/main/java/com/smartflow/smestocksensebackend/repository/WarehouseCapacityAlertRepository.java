package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.WarehouseCapacityAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseCapacityAlertRepository extends JpaRepository<WarehouseCapacityAlert, Long> {
    Optional<WarehouseCapacityAlert> findFirstByWarehouseIdAndStatusIn(Long warehouseId, List<InventoryAlertStatus> statuses);
}
