package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.service.InventoryAlertQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-alerts")
@RequiredArgsConstructor
@Validated
public class InventoryAlertController {

    private final InventoryAlertQueryService inventoryAlertQueryService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryAlertResponse>> getAlerts(
            @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false, defaultValue = "20") @Positive int size,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) InventoryAlertSeverity severity,
            @RequestParam(required = false) List<InventoryAlertStatus> status) {

        Pageable pageable = PageRequest.of(page, size);
        
        Page<InventoryAlertResponse> resultPage = inventoryAlertQueryService.getAlerts(
                warehouseId, 
                productId, 
                severity, 
                status, 
                pageable);

        return ResponseEntity.ok(PageResponse.from(resultPage));
    }
}
