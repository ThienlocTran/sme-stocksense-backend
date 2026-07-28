package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import org.springframework.stereotype.Component;

/**
 * Note: [T179 / T180 - Component] Component chịu trách nhiệm tính toán mức độ nghiêm trọng
 * (Severity) của phiếu cảnh báo tồn kho.
 * Tách biệt hoàn toàn (Separation of Concerns) khỏi InventoryAlertDetectionService để chuẩn bị
 * cho logic leo thang phức tạp và các quy tắc thông báo ở T180.
 */
@Component
public class AlertSeverityCalculator {

    private static final String STOCK_STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    /**
     * Tính toán Severity dựa vào số lượng tồn kho hiện tại và trạng thái mặt hàng.
     */
    public InventoryAlertSeverity calculate(int currentQuantity, int minStock, String status) {
        if (currentQuantity <= 0 || STOCK_STATUS_OUT_OF_STOCK.equals(status)) {
            return InventoryAlertSeverity.CRITICAL;
        }
        return InventoryAlertSeverity.WARNING;
    }
}
