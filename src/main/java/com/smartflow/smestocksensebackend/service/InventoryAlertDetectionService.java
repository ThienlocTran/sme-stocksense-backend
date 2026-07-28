package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inventory.AlertDetectionResultResponse;

/**
 * Note: [T178 - Service Interface] Service chuyên biệt phát hiện tồn kho thấp (Low Stock Detection Service).
 * Hỗ trợ 2 luồng hoạt động chuẩn theo nguyên tắc Balanced Architect:
 * 1. Batch Scan: Quét định kỳ (Cron Job / nút nhấn Dashboard) toàn bộ kho hoặc 1 kho cụ thể.
 * 2. Spot Check: Kiểm tra tức thời khi có giao dịch xuất kho làm tụt định mức.
 */
public interface InventoryAlertDetectionService {

    /**
     * Quét và tự động sinh phiếu cảnh báo cho các sản phẩm có tồn kho thấp (so_luong <= ton_toi_thieu)
     * hoặc hết hàng (so_luong <= 0).
     *
     * @param warehouseId ID kho hàng (null nếu muốn quét toàn bộ hệ thống)
     * @return DTO tổng kết kết quả quét (totalScanned, newAlertsCreated, existingAlertsSkipped)
     */
    AlertDetectionResultResponse scanAndCreateAlerts(Long warehouseId);

    /**
     * Kiểm tra điểm cho 1 cặp sản phẩm - kho hàng (Spot Check).
     *
     * @param productId   ID sản phẩm cần kiểm tra
     * @param warehouseId ID kho hàng
     * @return true nếu phát hiện tụt kho và đã sinh phiếu cảnh báo mới, false nếu không bị tụt kho hoặc đã bị bỏ qua (trùng lặp).
     */
    boolean checkAndCreateAlert(Long productId, Long warehouseId);

}
