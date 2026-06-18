package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ImportReceiptMappingTest {

    @Test
    void enumValues_shouldMatchT75Workflow() {
        List<String> values = Arrays.stream(ImportReceiptStatus.values())
                .map(Enum::name)
                .toList();

        assertEquals(List.of(
                "NHAP",
                "CHO_DUYET_CAP_1",
                "CHO_DUYET_CAP_2",
                "CHO_HANG_VE",
                "CHO_KIEM_HANG",
                "HOAN_THANH",
                "TU_CHOI",
                "HUY"
        ), values);
    }

    @Test
    void statusMapping_shouldUsePostgresNamedEnum() throws NoSuchFieldException {
        Field status = ImportReceipt.class.getDeclaredField("status");

        Enumerated enumerated = status.getAnnotation(Enumerated.class);
        Column column = status.getAnnotation(Column.class);

        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
        assertNotNull(column);
        assertEquals("trang_thai", column.name());
        assertEquals("trang_thai_chung_tu_kho", column.columnDefinition());
    }

    @Test
    void versionFields_shouldUseOptimisticLocking() throws NoSuchFieldException {
        assertNotNull(ImportReceipt.class.getDeclaredField("version").getAnnotation(Version.class));
        assertNotNull(ImportReceiptDetail.class.getDeclaredField("version").getAnnotation(Version.class));
    }

    @Test
    void quantityFields_shouldSeparateExpectedAndActualReceived() throws NoSuchFieldException {
        Column expected = ImportReceiptDetail.class.getDeclaredField("expectedQuantity").getAnnotation(Column.class);
        Column actual = ImportReceiptDetail.class.getDeclaredField("actualReceivedQuantity").getAnnotation(Column.class);

        assertEquals("so_luong", expected.name());
        assertEquals("so_luong_thuc_nhan", actual.name());
    }
}
