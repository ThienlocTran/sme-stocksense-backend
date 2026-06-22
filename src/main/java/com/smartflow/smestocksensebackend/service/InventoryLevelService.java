package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryFilterRequest;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryPageResponse;
import org.springframework.data.domain.Pageable;

public interface InventoryLevelService {

    /**
     * Lấy danh sách tồn kho có phân trang và lọc động theo {@link InventoryFilterRequest}.
     */
    InventoryPageResponse getInventoryPage(InventoryFilterRequest filter, Pageable pageable);
}
