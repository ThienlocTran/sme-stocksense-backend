package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryFilterRequest;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryPageResponse;
import com.smartflow.smestocksensebackend.service.InventoryLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryLevelService inventoryLevelService;

    /**
     * GET /api/inventories
     * Lấy danh sách tồn kho có phân trang + lọc động (warehouseId, categoryId,
     * keyword, minQuantity, maxQuantity).
     */
    @GetMapping
    public ResponseEntity<InventoryPageResponse> getInventories(
            @ModelAttribute InventoryFilterRequest filter,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryLevelService.getInventoryPage(filter, pageable));
    }
}
