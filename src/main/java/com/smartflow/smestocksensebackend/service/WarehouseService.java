package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;

import java.util.List;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ (Business Logic) liên quan đến
 * quản lý Kho Hàng.
 */
public interface WarehouseService {
    /**
     * Lấy danh sách kho hàng có hỗ trợ tìm kiếm động và lọc trạng thái.
     * 
     * @param keyword Từ khóa tìm kiếm tùy chọn (áp dụng tìm kiếm theo mã, tên, địa
     *                chỉ kho)
     * @param status  Trạng thái lọc tùy chọn (chỉ chấp nhận ACTIVE hoặc INACTIVE)
     * @return Danh sách DTO WarehouseResponse đại diện cho các kho hàng phù hợp
     *         điều kiện lọc
     */
    List<WarehouseResponse> getWarehouses(String keyword, String status);
}
