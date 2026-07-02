package com.smartflow.smestocksensebackend.entity;

/**
 * Loại giao dịch kho hàng.
 */
public enum InventoryTransactionType {
    NHAP_KHO,          // Nhập kho (từ phiếu nhập)
    XUAT_KHO,          // Xuất kho (từ phiếu xuất)
    NHAP_DAU_KY,       // Nhập tồn đầu kỳ (từ import Excel)
    DIEU_CHINH_TANG,   // Điều chỉnh tăng tồn kho
    DIEU_CHINH_GIAM    // Điều chỉnh giảm tồn kho
}
