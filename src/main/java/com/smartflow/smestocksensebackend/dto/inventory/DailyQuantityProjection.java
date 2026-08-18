package com.smartflow.smestocksensebackend.dto.inventory;

import java.time.LocalDate;

/**
 * Projection tổng số lượng giao dịch kho theo ngày, dùng để so sánh với dự báo AI (drift detection).
 */
public interface DailyQuantityProjection {
    LocalDate getNgay();

    Integer getTongSoLuong();
}
