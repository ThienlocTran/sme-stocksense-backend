package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventoryService {

    Page<InventoryLevelResponse> listInventory(Long warehouseId, Long productId, String keyword, String stockStatus,
            Pageable pageable);

}
