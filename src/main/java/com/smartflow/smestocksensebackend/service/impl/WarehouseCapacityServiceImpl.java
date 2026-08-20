package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseCapacityAlertRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository;
import com.smartflow.smestocksensebackend.service.WarehouseCapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarehouseCapacityServiceImpl implements WarehouseCapacityService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockConfigRepository warehouseStockConfigRepository;
    private final WarehouseCapacityAlertRepository warehouseCapacityAlertRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getUsedCapacity(Long warehouseId) {
        BigDecimal used = inventoryLevelRepository.sumUsedCapacityByWarehouseId(warehouseId);
        return (used != null ? used : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingCapacity(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        BigDecimal max = warehouse.getMaxCapacityM3() != null ? warehouse.getMaxCapacityM3() : new BigDecimal("1500.000");
        BigDecimal used = getUsedCapacity(warehouseId);
        return max.subtract(used).setScale(3, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getUsagePercentage(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        BigDecimal max = warehouse.getMaxCapacityM3() != null ? warehouse.getMaxCapacityM3() : new BigDecimal("1500.000");
        if (max.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal used = getUsedCapacity(warehouseId);
        return used.multiply(new BigDecimal("100")).divide(max, 2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMinimumSafeVolume(Long warehouseId) {
        BigDecimal safe = warehouseStockConfigRepository.sumMinimumSafeVolumeByWarehouseId(warehouseId);
        return (safe != null ? safe : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMissingSafeVolumeConfigCount(Long warehouseId) {
        return warehouseStockConfigRepository.countMissingSafeVolumeConfigByWarehouseId(warehouseId);
    }

    @Override
    @Transactional
    public WarehouseCapacityAlert evaluateCapacityAlert(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));

        BigDecimal max = warehouse.getMaxCapacityM3() != null ? warehouse.getMaxCapacityM3() : new BigDecimal("1500.000");
        BigDecimal used = getUsedCapacity(warehouseId);
        BigDecimal usagePercentage = BigDecimal.ZERO;
        if (max.compareTo(BigDecimal.ZERO) > 0) {
            usagePercentage = used.multiply(new BigDecimal("100")).divide(max, 2, RoundingMode.HALF_UP);
        }

        WarehouseCapacityAlertSeverity severity = null;
        if (usagePercentage.compareTo(new BigDecimal("80.00")) >= 0 && usagePercentage.compareTo(new BigDecimal("90.00")) < 0) {
            severity = WarehouseCapacityAlertSeverity.CAN_LUU_Y;
        } else if (usagePercentage.compareTo(new BigDecimal("90.00")) >= 0 && usagePercentage.compareTo(new BigDecimal("95.00")) < 0) {
            severity = WarehouseCapacityAlertSeverity.CAO;
        } else if (usagePercentage.compareTo(new BigDecimal("95.00")) >= 0 && usagePercentage.compareTo(new BigDecimal("100.00")) <= 0) {
            severity = WarehouseCapacityAlertSeverity.NGUY_HIEM;
        } else if (usagePercentage.compareTo(new BigDecimal("100.00")) > 0) {
            severity = WarehouseCapacityAlertSeverity.QUA_TAI;
        }

        Optional<WarehouseCapacityAlert> activeAlertOpt = warehouseCapacityAlertRepository
                .findFirstByWarehouseIdAndStatusIn(warehouseId, List.of(InventoryAlertStatus.OPEN, InventoryAlertStatus.ACKNOWLEDGED));

        if (severity == null) {
            // Tỷ lệ < 80%: giải quyết cảnh báo đang mở nếu có
            if (activeAlertOpt.isPresent()) {
                WarehouseCapacityAlert alert = activeAlertOpt.get();
                alert.resolve("System");
                alert.setMessage("Sức chứa kho đã về mức bình thường: " + usagePercentage + "%");
                return warehouseCapacityAlertRepository.save(alert);
            }
            return null;
        }

        // Tỷ lệ >= 80%: tạo mới hoặc cập nhật cảnh báo
        if (activeAlertOpt.isPresent()) {
            WarehouseCapacityAlert alert = activeAlertOpt.get();
            alert.setUsedCapacityM3(used);
            alert.setMaxCapacityM3(max);
            alert.setUsagePercentage(usagePercentage);
            alert.setSeverity(severity);
            alert.setMessage("Cập nhật cảnh báo sức chứa kho ở mức " + severity + ": " + usagePercentage + "%");
            return warehouseCapacityAlertRepository.save(alert);
        } else {
            WarehouseCapacityAlert alert = WarehouseCapacityAlert.builder()
                    .warehouse(warehouse)
                    .usedCapacityM3(used)
                    .maxCapacityM3(max)
                    .usagePercentage(usagePercentage)
                    .severity(severity)
                    .status(InventoryAlertStatus.OPEN)
                    .message("Cảnh báo sức chứa kho đạt mức " + severity + ": " + usagePercentage + "%")
                    .build();
            return warehouseCapacityAlertRepository.save(alert);
        }
    }
}
