package com.smartflow.smestocksensebackend.entity;

/**
 * Note: [T177 - Khối Enum] Định nghĩa vòng đời xử lý phiếu cảnh báo tồn kho.
 * - OPEN: Cảnh báo mới sinh tự động khi phát hiện tụt kho, chưa có ai xử lý.
 * - ACKNOWLEDGED: Quản lý kho hoặc Admin đã ghi nhận và đang lên đơn nhập/xử lý.
 * - RESOLVED: Hàng đã được nhập về đủ định mức hoặc đã giải quyết xong rủi ro.
 */
public enum InventoryAlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED
}
