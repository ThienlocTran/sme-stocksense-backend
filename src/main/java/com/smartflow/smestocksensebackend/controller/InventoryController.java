package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public Page<InventoryLevelResponse> listInventory(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "status", required = false) String stockStatus,
            @RequestParam(required = false) String warehouseStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return inventoryService.listInventory(warehouseId, productId, keyword, stockStatus,
                warehouseStatus, productStatus, pageable);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public Page<InventoryLevelResponse> listLowStockInventory(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String warehouseStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return inventoryService.listInventory(warehouseId, productId, keyword, "LOW_STOCK",
                warehouseStatus, productStatus, pageable);
    }
}
