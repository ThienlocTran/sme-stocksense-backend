package com.smartflow.smestocksensebackend.entity;

/**
 * Chế độ tính toán được dùng để tạo ra một kết quả dự báo.
 * - XGBOOST: đủ dữ liệu (>= MIN_HISTORY_DAYS), gọi sang AI forecast service.
 * - COLD_START_AVG: chưa đủ dữ liệu, dùng trung bình động (moving average) làm fallback.
 */
public enum ForecastMode {
    XGBOOST,
    COLD_START_AVG
}
