package com.smartflow.smestocksensebackend.entity;

/**
 * Trạng thái phiếu xuất kho – duyệt 2 cấp.
 * Luồng chính: DRAFT → PENDING_APPROVAL_L1 → PENDING_APPROVAL_L2 → COMPLETED
 * Luồng từ chối:       ↘ REJECTED (cho phép sửa rồi gửi lại)
 *
 * NOTE: Cần migration thêm các giá trị này vào DB enum 'trang_thai_chung_tu_kho'.
 */
public enum ExportReceiptStatus {
    DRAFT,                  // Nháp – cho phép chỉnh sửa / thêm item
    PENDING_APPROVAL_L1,    // Chờ duyệt cấp 1
    PENDING_APPROVAL_L2,    // Chờ duyệt cấp 2
    COMPLETED,              // Hoàn thành – đã trừ tồn kho
    REJECTED                // Từ chối – cho phép sửa lại / thêm item
}
