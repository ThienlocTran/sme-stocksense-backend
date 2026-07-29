package com.smartflow.smestocksensebackend.entity;

/**
 * Định nghĩa mức độ nghiêm trọng của cảnh báo tồn kho.
 * Kế thừa chuẩn xác 100% từ Single Source of Truth (SSOT) tại T176.
 * - CRITICAL: Tồn kho thực tế <= 0 (Hết hàng hoàn toàn hoặc tồn âm).
 * - WARNING: 0 < Tồn kho <= Tồn tối thiểu (Sắp hết hàng).
 */
public enum InventoryAlertSeverity {
    CRITICAL,
    WARNING
}
