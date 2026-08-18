package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.DriftResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;

public interface ForecastService {

    /**
     * Chạy dự báo đồng bộ cho 1 sản phẩm/kho: đủ dữ liệu -> gọi AI service (XGBoost),
     * thiếu dữ liệu -> cold-start (trung bình động). Lưu kết quả mới (tăng version).
     */
    ForecastResponse runForecast(Long productId, Long warehouseId);

    /** Lấy kết quả dự báo mới nhất đã lưu, không train lại. */
    ForecastResponse getLatestForecast(Long productId, Long warehouseId);

    /** So sánh dự báo đã lưu (horizon 7 ngày) với thực tế xuất kho 30 ngày gần nhất. */
    DriftResponse checkDrift(Long productId, Long warehouseId);

    /** Công cụ demo: sinh dữ liệu lịch sử bán hàng giả lập cho các sản phẩm/kho chưa đủ dữ liệu. */
    SeedHistoryResponse seedHistory();
}
