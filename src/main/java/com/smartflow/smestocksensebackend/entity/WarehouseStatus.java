package com.smartflow.smestocksensebackend.entity;

/**
 * Trạng thái hoạt động của kho hàng.
 * Hệ thống không xóa vật lý kho để bảo toàn lịch sử nhập/xuất/tồn kho — chỉ đổi trạng thái.
 */
public enum WarehouseStatus {
    HOAT_DONG,       // Kho đang hoạt động, cho phép nhập xuất và lưu trữ hàng hóa
    NGUNG_HOAT_DONG  // Kho tạm ngừng hoạt động, không cho phép tạo giao dịch mới
}
