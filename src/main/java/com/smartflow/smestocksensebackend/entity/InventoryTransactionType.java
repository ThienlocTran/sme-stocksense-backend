package com.smartflow.smestocksensebackend.entity;

/**
 * Loại giao dịch kho, ánh xạ tới Postgres enum type "loai_giao_dich_kho"
 * trên cột giao_dich_kho.loai_giao_dich.
 */
public enum InventoryTransactionType {
    NHAP_KHO,
    XUAT_KHO,
    NHAP_DAU_KY,
    DIEU_CHINH_TANG,
    DIEU_CHINH_GIAM
}
