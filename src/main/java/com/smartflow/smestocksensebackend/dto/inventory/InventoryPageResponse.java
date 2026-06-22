package com.smartflow.smestocksensebackend.dto.inventory;

import org.springframework.data.domain.Page;

import java.util.List;

public record InventoryPageResponse(
        List<InventoryListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static InventoryPageResponse from(Page<InventoryListItemResponse> inventoryPage) {
        return new InventoryPageResponse(
                inventoryPage.getContent(),
                inventoryPage.getNumber(),
                inventoryPage.getSize(),
                inventoryPage.getTotalElements(),
                inventoryPage.getTotalPages()
        );
    }
}
