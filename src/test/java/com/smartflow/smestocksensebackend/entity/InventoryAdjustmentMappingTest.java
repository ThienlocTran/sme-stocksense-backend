package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InventoryAdjustmentMappingTest {

    @Test
    void enumValues_shouldMatchFrontendLifecycle() {
        List<String> values = Arrays.stream(InventoryAdjustmentStatus.values())
                .map(Enum::name)
                .toList();

        assertEquals(List.of("NHAP", "CHO_DUYET", "DA_DUYET", "TU_CHOI", "DA_AP_DUNG"), values);
    }

    @Test
    void headerMapping_shouldUseInventoryCountAndEmployeeRelationships() throws NoSuchFieldException {
        assertEquals("phieu_dieu_chinh_kiem_ke",
                InventoryAdjustment.class.getAnnotation(Table.class).name());

        assertJoinColumn("inventoryCount", "dot_kiem_ke_id");
        assertJoinColumn("createdBy", "nguoi_tao_id");
        assertJoinColumn("submittedBy", "nguoi_gui_duyet_id");
        assertJoinColumn("approvedBy", "nguoi_duyet_id");

        Field status = InventoryAdjustment.class.getDeclaredField("status");
        assertEquals(EnumType.STRING, status.getAnnotation(Enumerated.class).value());
        assertEquals("trang_thai", status.getAnnotation(Column.class).name());
        assertEquals(20, status.getAnnotation(Column.class).length());

        assertNotNull(InventoryAdjustment.class.getDeclaredField("version").getAnnotation(Version.class));
    }

    @Test
    void header_shouldNotExposeDuplicatedLineRelationship() {
        assertThrows(NoSuchFieldException.class, () -> InventoryAdjustment.class.getDeclaredField("lines"));
    }

    @Test
    void countDetail_shouldPersistReasonSeparatelyFromNote() throws NoSuchFieldException {
        assertEquals("ly_do_chenh_lech",
                InventoryCountDetail.class.getDeclaredField("reason").getAnnotation(Column.class).name());
        assertEquals("ghi_chu",
                InventoryCountDetail.class.getDeclaredField("note").getAnnotation(Column.class).name());
    }

    private void assertJoinColumn(String fieldName, String columnName) throws NoSuchFieldException {
        assertEquals(columnName,
                InventoryAdjustment.class.getDeclaredField(fieldName).getAnnotation(JoinColumn.class).name());
    }

}
