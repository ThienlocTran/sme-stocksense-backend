package com.smartflow.smestocksensebackend.entity;

/**
 * Loại đối tác: dùng để phân biệt nhà cung cấp (NHA_CUNG_CAP), khách hàng (KHACH_HANG) hoặc cả hai (CA_HAI).
 * Ghi chú nghiệp vụ: validate loại đối tác để tránh phát sinh dữ liệu sai trong nghiệp vụ nhập/xuất kho sau này.
 */
public enum PartnerType {
    NHA_CUNG_CAP,
    KHACH_HANG,
    CA_HAI
}
