package com.smartflow.smestocksensebackend.dto.response;

import com.smartflow.smestocksensebackend.entity.Warehouse;

/**
 * Data Transfer Object (DTO) đại diện cho dữ liệu thông tin kho hàng trả về phía Client.
 * Dùng cấu trúc Record của Java giúp định nghĩa nhanh một lớp dữ liệu bất biến (immutable).
 */
public record WarehouseResponse(
        Long id,        // ID định danh của kho hàng
        String code,    // Mã kho hàng
        String name,    // Tên kho hàng
        String address, // Địa chỉ kho hàng
        String status   // Chuỗi trạng thái hoạt động (ACTIVE/INACTIVE)
) {
    /**
     * Chuyển đổi từ thực thể Warehouse Entity gốc sang đối tượng đóng gói WarehouseResponse DTO.
     * @param warehouse Thực thể Warehouse lấy ra từ Database
     * @return DTO WarehouseResponse chứa dữ liệu cần thiết trả về cho Client
     */
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),              // Lấy ID kho hàng
                warehouse.getCode(),            // Lấy mã kho
                warehouse.getName(),            // Lấy tên kho
                warehouse.getAddress(),         // Lấy địa chỉ kho hàng
                warehouse.getStatus().name()    // Chuyển đổi kiểu Enum của Entity sang kiểu String cho DTO
        );
    }
}
