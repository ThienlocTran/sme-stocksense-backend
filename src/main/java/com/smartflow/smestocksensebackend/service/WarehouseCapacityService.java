package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.WarehouseCapacityAlert;
import com.smartflow.smestocksensebackend.dto.replenishment.WarehouseCapacityAvailability;
import java.math.BigDecimal;

public interface WarehouseCapacityService {
    BigDecimal getUsedCapacity(Long warehouseId);
    BigDecimal getRemainingCapacity(Long warehouseId);
    WarehouseCapacityAvailability getAvailability(Long warehouseId, BigDecimal targetProductUnitVolumeM3);
    BigDecimal getUsagePercentage(Long warehouseId);
    BigDecimal getMinimumSafeVolume(Long warehouseId);
    long getMissingSafeVolumeConfigCount(Long warehouseId);
    WarehouseCapacityAlert evaluateCapacityAlert(Long warehouseId);
}
