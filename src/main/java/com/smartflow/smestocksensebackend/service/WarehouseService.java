package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.request.CreateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;

import java.util.List;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ (Business Logic) liên quan đến quản lý Kho Hàng.
 */
public interface WarehouseService {

    /**
     * Lấy danh sách kho hàng có hỗ trợ tìm kiếm động và lọc trạng thái.
     *
     * @param keyword Từ khóa tìm kiếm tùy chọn (mã, tên hoặc địa chỉ kho)
     * @param status  Trạng thái lọc tùy chọn (ACTIVE hoặc INACTIVE)
     * @return Danh sách DTO WarehouseResponse đại diện cho các kho hàng phù hợp điều kiện lọc
     */
    List<WarehouseResponse> getWarehouses(String keyword, String status);

    /**
     * Nghiệp vụ thêm kho hàng mới vào hệ thống.
     * Thực hiện validate trùng mã kho và chuẩn hóa dữ liệu trước khi lưu vào CSDL.
     *
     * @param request DTO chứa thông tin kho hàng cần tạo
     * @return DTO WarehouseResponse đại diện cho kho hàng vừa được tạo thành công
     */
    WarehouseResponse createWarehouse(CreateWarehouseRequest request);

    /**
     * Nghiệp vụ cập nhật thông tin kho hàng.
     * Chỉ cho phép sửa tên kho, địa chỉ và trạng thái. Không cho phép sửa mã kho.
     *
     * @param id      ID của kho hàng cần cập nhật
     * @param request DTO chứa thông tin cập nhật mới
     * @return DTO WarehouseResponse đại diện cho kho hàng sau khi cập nhật thành công
     */
    WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request);
}
