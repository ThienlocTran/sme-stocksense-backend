package com.smartflow.smestocksensebackend.dto.response;

import com.smartflow.smestocksensebackend.entity.Warehouse;

/**
 * DTO trả về thông tin kho hàng cho Client.
 * Tên field dùng camelCase tương ứng với tên cột tiếng Việt không dấu trong DB.
 */
public record WarehouseResponse(
        Long id,
        String maKho,
        String tenKho,
        String diaChi,
        String trangThai,
        java.math.BigDecimal maxCapacityM3
) {
    /**
     * Chuyển đổi từ Entity Warehouse sang DTO WarehouseResponse.
     */
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getStatus().name(),
                warehouse.getMaxCapacityM3()
        );
    }
}
