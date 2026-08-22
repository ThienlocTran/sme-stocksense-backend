package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.forecast.DriftResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.service.ForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dự báo nhu cầu tồn kho bằng AI (XGBoost) - tính năng độc lập với InventoryAlert
 * (reactive, dựa snapshot hiện tại) và ReplenishmentSuggestion (rule-based min/max).
 */
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    @PostMapping("/{productId}/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ForecastResponse> runForecast(@PathVariable Long productId,
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "EXTERNAL_STORE_ITEM") SalesHistorySource source) {
        return ResponseEntity.ok(forecastService.runForecast(productId, warehouseId, source));
    }

    /**
     * Trả 200 + body khi đã có kết quả dự báo lưu sẵn.
     * Trả 204 No Content khi SP/Kho hợp lệ nhưng chưa từng chạy dự báo – không phải lỗi.
     */
    @GetMapping("/{productId}/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ForecastResponse> getLatestForecast(@PathVariable Long productId,
            @PathVariable Long warehouseId) {
        try {
            return ResponseEntity.ok(forecastService.getLatestForecast(productId, warehouseId));
        } catch (NotFoundException e) {
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/{productId}/{warehouseId}/drift")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<DriftResponse> checkDrift(@PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(forecastService.checkDrift(productId, warehouseId));
    }

    @PostMapping("/seed-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<SeedHistoryResponse> seedHistory() {
        return ResponseEntity.ok(forecastService.seedDemoHistory());
    }
}
