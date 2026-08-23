package com.smartflow.smestocksensebackend.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryAdjustmentMigrationTest {

    private static final Path V51 = Path.of(
            "src/main/resources/db/migration/V51__create_inventory_adjustment_tables.sql"
    );
    private static final Path V52 = Path.of(
            "src/main/resources/db/migration/V52__normalize_inventory_adjustment_domain.sql"
    );

    @Test
    void v51_shouldRemainHistoricalHeaderAndLineTableMigration() throws IOException {
        String sql = Files.readString(V51);

        assertTrue(sql.contains("CREATE TABLE phieu_dieu_chinh_kiem_ke"));
        assertTrue(sql.contains("CREATE TABLE chi_tiet_dieu_chinh_kiem_ke"));
        assertTrue(sql.contains("dot_kiem_ke_id BIGINT NOT NULL REFERENCES dot_kiem_ke(id)"));
        assertTrue(sql.contains("phieu_dieu_chinh_id BIGINT NOT NULL REFERENCES phieu_dieu_chinh_kiem_ke(id) ON DELETE CASCADE"));
        assertTrue(sql.contains("san_pham_id BIGINT NOT NULL REFERENCES san_pham(id)"));
    }

    @Test
    void v51_shouldPersistLifecycleAndActivePartialUniqueIndex() throws IOException {
        String sql = Files.readString(V51);

        assertTrue(sql.contains("'NHAP','CHO_DUYET','DA_DUYET','TU_CHOI','DA_AP_DUNG'"));
        assertTrue(sql.contains("WHERE trang_thai IN ('NHAP','CHO_DUYET','DA_DUYET')"));
        assertTrue(sql.contains("uk_phieu_dieu_chinh_kiem_ke_active"));
        assertFalse(sql.contains("WHERE trang_thai IN ('NHAP','CHO_DUYET','DA_DUYET','TU_CHOI','DA_AP_DUNG')"));
    }

    @Test
    void v52_shouldAddReasonAndHeaderOneToOneWithoutDroppingLineTable() throws IOException {
        String sql = Files.readString(V52);

        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS ly_do_chenh_lech VARCHAR(255)"));
        assertTrue(sql.contains("CREATE UNIQUE INDEX IF NOT EXISTS uk_phieu_dieu_chinh_kiem_ke_dot"));
        assertTrue(sql.contains("ON phieu_dieu_chinh_kiem_ke(dot_kiem_ke_id)"));
        assertFalse(sql.contains("DROP TABLE"));
        assertFalse(sql.contains("chi_tiet_dieu_chinh_kiem_ke"));
        assertFalse(sql.contains("ton_kho"));
        assertFalse(sql.contains("giao_dich_kho"));
    }
}
