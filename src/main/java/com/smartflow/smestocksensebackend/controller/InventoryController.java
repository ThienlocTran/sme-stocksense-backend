package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public Page<InventoryLevelResponse> listInventory(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return inventoryService.listInventory(warehouseId, productId, keyword, stockStatus, pageable);
    }
}
