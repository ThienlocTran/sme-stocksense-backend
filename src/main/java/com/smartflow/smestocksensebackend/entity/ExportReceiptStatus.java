package com.smartflow.smestocksensebackend.entity;

public enum ExportReceiptStatus {
    NHAP,
    CHO_DUYET_CAP_1,
    CHO_DUYET_CAP_2,
    CHO_XUAT,
    HOAN_THANH,
    TU_CHOI,
    HUY;

    public String approvalLevel() {
        return switch (this) {
            case CHO_DUYET_CAP_1 -> "LEVEL_1";
            case CHO_DUYET_CAP_2 -> "LEVEL_2";
            default -> null;
        };
    }
}
