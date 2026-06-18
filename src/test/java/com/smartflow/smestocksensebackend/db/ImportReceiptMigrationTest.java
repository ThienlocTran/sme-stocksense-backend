package com.smartflow.smestocksensebackend.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportReceiptMigrationTest {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void migrations_shouldAddT75StatusesAndWorkflowColumns() throws IOException {
        String v7 = Files.readString(MIGRATION_DIR.resolve("V7__align_import_receipt_workflow.sql"));
        String v8 = Files.readString(MIGRATION_DIR.resolve("V8__add_import_receipt_workflow_columns.sql"));

        assertTrue(v7.contains("'CHO_DUYET_CAP_1'"));
        assertTrue(v7.contains("'CHO_DUYET_CAP_2'"));
        assertTrue(v7.contains("'CHO_HANG_VE'"));
        assertTrue(v7.contains("'CHO_KIEM_HANG'"));

        assertTrue(v8.contains("\"nguoi_duyet_cap_1_id\""));
        assertTrue(v8.contains("\"nguoi_duyet_cap_2_id\""));
        assertTrue(v8.contains("\"nguoi_huy_id\""));
        assertTrue(v8.contains("\"nguoi_hoan_thanh_id\""));
        assertTrue(v8.contains("\"so_luong_thuc_nhan\""));
        assertTrue(v8.contains("\"version\" bigint NOT NULL DEFAULT 0"));
    }

    @Test
    void migration_shouldDeclareExpectedConstraintsAndIndexes() throws IOException {
        String v8 = Files.readString(MIGRATION_DIR.resolve("V8__add_import_receipt_workflow_columns.sql"));

        assertTrue(v8.contains("chk_ct_phieu_nhap_so_luong_positive"));
        assertTrue(v8.contains("CHECK (\"so_luong\" > 0)"));
        assertTrue(v8.contains("chk_ct_phieu_nhap_so_luong_thuc_nhan_non_negative"));
        assertTrue(v8.contains("chk_ct_phieu_nhap_don_gia_non_negative"));
        assertTrue(v8.contains("chk_ct_phieu_nhap_thanh_tien_non_negative"));
        assertTrue(v8.contains("chk_phieu_nhap_tong_tien_non_negative"));

        assertTrue(v8.contains("idx_phieu_nhap_trang_thai"));
        assertTrue(v8.contains("idx_phieu_nhap_kho_id"));
        assertTrue(v8.contains("idx_phieu_nhap_doi_tac_id"));
        assertTrue(v8.contains("idx_phieu_nhap_nguoi_tao_id"));
        assertTrue(v8.contains("idx_phieu_nhap_ngay_tao"));
    }
}
