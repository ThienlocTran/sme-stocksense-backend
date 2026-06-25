package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExcelImportMappingTest {

    @Test
    void enumValues_shouldMatchImportStatusType() {
        List<String> values = Arrays.stream(ExcelImportStatus.values())
                .map(Enum::name)
                .toList();

        assertEquals(List.of(
                "CHO_XU_LY",
                "CO_LOI",
                "SAN_SANG_IMPORT",
                "DA_IMPORT",
                "THAT_BAI"
        ), values);
    }

    @Test
    void importSession_shouldMapToExistingTableAndStatusEnum() throws NoSuchFieldException {
        Table table = ExcelImport.class.getAnnotation(Table.class);
        Field status = ExcelImport.class.getDeclaredField("status");

        Enumerated enumerated = status.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = status.getAnnotation(JdbcTypeCode.class);
        Column column = status.getAnnotation(Column.class);

        assertNotNull(table);
        assertEquals("lan_import_excel", table.name());
        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
        assertNotNull(jdbcTypeCode);
        assertEquals(SqlTypes.NAMED_ENUM, jdbcTypeCode.value());
        assertNotNull(column);
        assertEquals("trang_thai", column.name());
        assertEquals("trang_thai_import", column.columnDefinition());
    }

    @Test
    void importError_shouldMapToExistingTableAndImportSessionFk() throws NoSuchFieldException {
        Table table = ExcelImportError.class.getAnnotation(Table.class);
        JoinColumn importJoin = ExcelImportError.class.getDeclaredField("excelImport").getAnnotation(JoinColumn.class);

        assertNotNull(table);
        assertEquals("loi_import_excel", table.name());
        assertNotNull(importJoin);
        assertEquals("lan_import_id", importJoin.name());
    }

    @Test
    void defaultImportType_shouldStayOpeningInventoryMvpScope() {
        ExcelImport excelImport = new ExcelImport();

        assertEquals("TON_DAU_KY", excelImport.getImportType());
    }
}
