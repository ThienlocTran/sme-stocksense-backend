package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;

/**
 * Service chuyên xử lý các thao tác Write/Action đối với Cảnh báo tồn kho.
 * Phân tách theo mô hình CQRS Lite.
 */
public interface InventoryAlertActionService {

    /**
     * Đánh dấu cảnh báo đã tiếp nhận (Acknowledge).
     * 
     * @param id ID của cảnh báo.
     * @return Thông tin cảnh báo sau khi cập nhật.
     */
    InventoryAlertResponse acknowledgeAlert(Long id);
}
