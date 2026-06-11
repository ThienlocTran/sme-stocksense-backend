package com.smartflow.smestocksensebackend.entity;

/**
 * Trạng thái hoạt động của đối tác (HOAT_DONG hoặc NGUNG_HOAT_DONG).
 * Nghiệp vụ: Hệ thống không xóa vật lý đối tác để đảm bảo lịch sử giao dịch.
 */
public enum PartnerStatus {
    HOAT_DONG,
    NGUNG_HOAT_DONG
}
