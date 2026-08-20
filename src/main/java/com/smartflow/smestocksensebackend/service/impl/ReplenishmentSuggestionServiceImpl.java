package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.dto.replenishment.*;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.ReplenishmentSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplenishmentSuggestionServiceImpl implements ReplenishmentSuggestionService {
    private final InventoryLevelRepository inventoryLevelRepository;
    private final com.smartflow.smestocksensebackend.service.WarehouseCapacityService warehouseCapacityService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReplenishmentSuggestionResponse> listSuggestions(Long warehouseId, Long productId, String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        return inventoryLevelRepository.findInventory(warehouseId, productId, normalizedKeyword, "LOW_STOCK",
                "HOAT_DONG", "HOAT_DONG", pageable).map(this::toSuggestion);
    }

    private ReplenishmentSuggestionResponse toSuggestion(InventoryLevelProjection stock) {
        int current = nonNegative(stock.getCurrentQuantity());
        int minimum = nonNegative(stock.getMinStock());
        int shortage = Math.max(minimum - current, 0);
        int suggested = shortage;
        String warning = null;

        java.math.BigDecimal unitVolumeM3 = null;
        Integer capacityAllowedQuantity = suggested;
        Boolean capacityLimited = false;

        unitVolumeM3 = stock.getUnitVolumeM3();

        if (unitVolumeM3 == null) {
            warning = "UNIT_VOLUME_NOT_CONFIGURED";
        } else if (unitVolumeM3.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal remaining = warehouseCapacityService.getRemainingCapacity(stock.getWarehouseId());
            java.math.BigDecimal limit = remaining.divide(unitVolumeM3, 0, java.math.RoundingMode.DOWN);
            int maxFit = Math.max(0, limit.intValue());
            if (suggested > maxFit) {
                capacityAllowedQuantity = maxFit;
                capacityLimited = true;
                warning = String.format("Đề xuất nhập %d đơn vị. Kho hiện chỉ còn đủ sức chứa cho khoảng %d đơn vị.", suggested, maxFit);
            }
        }

        ReplenishmentReason reason = current == 0 ? ReplenishmentReason.OUT_OF_STOCK
                : current < minimum ? ReplenishmentReason.BELOW_MINIMUM : ReplenishmentReason.AT_MINIMUM;
        ReplenishmentPriority priority = current == 0 ? ReplenishmentPriority.CRITICAL
                : current < minimum ? ReplenishmentPriority.HIGH : ReplenishmentPriority.MEDIUM;
        return new ReplenishmentSuggestionResponse(stock.getProductId(), stock.getProductCode(), stock.getProductName(),
                stock.getWarehouseId(), stock.getWarehouseCode(), stock.getWarehouse(), current, minimum, null,
                shortage, suggested, reason, priority, warning, unitVolumeM3, capacityAllowedQuantity, capacityLimited);
    }

    private int nonNegative(Integer value) { return value == null ? 0 : Math.max(value, 0); }
}
