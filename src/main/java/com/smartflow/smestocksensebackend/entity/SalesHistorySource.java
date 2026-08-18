package com.smartflow.smestocksensebackend.entity;

/**
 * Nguồn gốc của một điểm dữ liệu trong lịch sử bán hàng dùng để huấn luyện mô hình dự báo.
 * - SEED: dữ liệu giả lập sinh ra để demo (chưa đủ giao dịch thực tế).
 * - THUC_TE: tổng hợp từ giao dịch xuất kho thực tế (giao_dich_kho).
 */
public enum SalesHistorySource {
    SEED,
    THUC_TE
}
