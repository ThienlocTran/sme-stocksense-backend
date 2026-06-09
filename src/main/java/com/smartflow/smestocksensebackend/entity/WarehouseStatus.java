package com.smartflow.smestocksensebackend.entity;

/**
 * Enum đại diện cho các trạng thái hoạt động của kho hàng (Warehouse).
 * ACTIVE: Đang hoạt động.
 * INACTIVE: Ngừng hoạt động.
 */
public enum WarehouseStatus {
    // Trạng thái kho hàng đang hoạt động bình thường, cho phép nhập xuất và lưu kho
    ACTIVE,
    
    // Trạng thái kho hàng ngưng hoạt động, tạm ngưng các giao dịch nhập xuất
    INACTIVE
}
