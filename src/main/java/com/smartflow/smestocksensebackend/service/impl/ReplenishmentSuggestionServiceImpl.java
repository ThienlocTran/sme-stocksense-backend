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
        Integer maximum = stock.getMaxStock();
        int shortage = Math.max(minimum - current, 0);
        String warning = null;
        int suggested;
        if (maximum == null) {
            suggested = shortage;
            warning = "MAX_STOCK_NOT_CONFIGURED";
        } else if (maximum < minimum || maximum < current) {
            suggested = shortage;
            warning = "INVALID_STOCK_RANGE";
        } else {
            suggested = maximum - current;
        }
        ReplenishmentReason reason = current == 0 ? ReplenishmentReason.OUT_OF_STOCK
                : current < minimum ? ReplenishmentReason.BELOW_MINIMUM : ReplenishmentReason.AT_MINIMUM;
        ReplenishmentPriority priority = current == 0 ? ReplenishmentPriority.CRITICAL
                : current < minimum ? ReplenishmentPriority.HIGH : ReplenishmentPriority.MEDIUM;
        return new ReplenishmentSuggestionResponse(stock.getProductId(), stock.getProductCode(), stock.getProductName(),
                stock.getWarehouseId(), stock.getWarehouseCode(), stock.getWarehouse(), current, minimum, maximum,
                shortage, suggested, reason, priority, warning);
    }

    private int nonNegative(Integer value) { return value == null ? 0 : Math.max(value, 0); }
}
