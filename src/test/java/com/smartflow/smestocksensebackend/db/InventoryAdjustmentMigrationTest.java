package com.smartflow.smestocksensebackend.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryAdjustmentMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/V51__create_inventory_adjustment_tables.sql"
    );

    @Test
    void migration_shouldCreateHeaderAndLineTables() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("CREATE TABLE phieu_dieu_chinh_kiem_ke"));
        assertTrue(sql.contains("CREATE TABLE chi_tiet_dieu_chinh_kiem_ke"));
        assertTrue(sql.contains("dot_kiem_ke_id BIGINT NOT NULL REFERENCES dot_kiem_ke(id)"));
        assertTrue(sql.contains("phieu_dieu_chinh_id BIGINT NOT NULL REFERENCES phieu_dieu_chinh_kiem_ke(id) ON DELETE CASCADE"));
        assertTrue(sql.contains("san_pham_id BIGINT NOT NULL REFERENCES san_pham(id)"));
    }

    @Test
    void migration_shouldPersistLifecycleAndHistoricalStatuses() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("'NHAP','CHO_DUYET','DA_DUYET','TU_CHOI','DA_AP_DUNG'"));
        assertTrue(sql.contains("WHERE trang_thai IN ('NHAP','CHO_DUYET','DA_DUYET')"));
        assertTrue(sql.contains("uk_phieu_dieu_chinh_kiem_ke_active"));
        assertFalse(sql.contains("WHERE trang_thai IN ('NHAP','CHO_DUYET','DA_DUYET','TU_CHOI','DA_AP_DUNG')"));
    }

    @Test
    void migration_shouldSnapshotDiscrepancyAndAvoidStockMutationTables() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertTrue(sql.contains("so_luong_he_thong INTEGER NOT NULL"));
        assertTrue(sql.contains("so_luong_thuc_te INTEGER NOT NULL"));
        assertTrue(sql.contains("chenh_lech INTEGER NOT NULL"));
        assertTrue(sql.contains("CHECK (chenh_lech = so_luong_thuc_te - so_luong_he_thong)"));
        assertFalse(sql.contains("ton_kho"));
        assertFalse(sql.contains("giao_dich_kho"));
    }
}
