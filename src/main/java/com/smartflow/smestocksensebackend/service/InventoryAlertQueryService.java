package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryAlertQueryService {

    /**
     * Lấy danh sách phiếu cảnh báo tồn kho có phân trang và lọc.
     * Default sort: business priority (CRITICAL -> WARNING) sau đó createdAt DESC.
     * Default status (nếu null/rỗng): OPEN, ACKNOWLEDGED.
     */
    Page<InventoryAlertResponse> getAlerts(
            Long warehouseId,
            Long productId,
            InventoryAlertSeverity severity,
            List<InventoryAlertStatus> statuses,
            Pageable pageable);
}
