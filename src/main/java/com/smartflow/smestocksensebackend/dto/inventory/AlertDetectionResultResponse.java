package com.smartflow.smestocksensebackend.dto.inventory;

import java.time.LocalDateTime;

/**
 * Record tổng kết kết quả sau khi thực hiện quét (scan) phát hiện cảnh báo tồn
 * kho thấp.
 * Trả về Summary thay vì danh sách chi tiết để tránh tải nặng bộ nhớ và dễ dàng
 * log trong Cron Job / Dashboard.
 */
public record AlertDetectionResultResponse(
        int totalScanned,
        int newAlertsCreated,
        int existingAlertsUpdated,
        int existingAlertsUnchanged,
        int raceConditionIgnored,
        LocalDateTime timestamp) {

    /**
     * Tổng số phiếu cũ bị bỏ qua không tạo mới (bao gồm cập nhật số lượng, giữ nguyên, hoặc bỏ qua do race condition).
     */
    public int existingAlertsSkipped() {
        return existingAlertsUpdated + existingAlertsUnchanged + raceConditionIgnored;
    }

    /**
     * Helper tạo nhanh kết quả với thời gian hiện tại.
     */
    public static AlertDetectionResultResponse of(int totalScanned, int newAlertsCreated,
            int existingAlertsUpdated, int existingAlertsUnchanged, int raceConditionIgnored) {
        return new AlertDetectionResultResponse(totalScanned, newAlertsCreated, existingAlertsUpdated,
                existingAlertsUnchanged, raceConditionIgnored, LocalDateTime.now());
    }

    /**
     * Helper tạo kết quả rỗng khi không có mặt hàng nào cần quét hoặc kho rỗng.
     */
    public static AlertDetectionResultResponse empty() {
        return new AlertDetectionResultResponse(0, 0, 0, 0, 0, LocalDateTime.now());
    }
}
