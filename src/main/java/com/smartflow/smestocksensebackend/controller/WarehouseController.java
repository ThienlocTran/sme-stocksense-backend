package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.request.CreateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller tiếp nhận các yêu cầu HTTP bên ngoài liên quan đến quản lý Kho Hàng.
 */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * API: GET /api/warehouses
     * Trả về danh sách kho hàng dựa trên từ khóa tìm kiếm động và trạng thái hoạt động.
     *
     * @param keyword Từ khóa tìm kiếm tùy chọn (mã, tên hoặc địa chỉ)
     * @param status  Trạng thái hoạt động lọc tùy chọn (ACTIVE hoặc INACTIVE)
     */
    @GetMapping
    public List<WarehouseResponse> getWarehouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return warehouseService.getWarehouses(keyword, status);
    }

    /**
     * API: POST /api/warehouses
     * Tiếp nhận yêu cầu thêm mới một kho hàng và thực hiện kiểm tra tính hợp lệ của dữ liệu đầu vào.
     *
     * @param request DTO chứa thông tin kho hàng cần tạo mới
     * @return DTO chứa thông tin chi tiết của kho hàng vừa được tạo thành công
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse createWarehouse(@Valid @RequestBody CreateWarehouseRequest request) {
        return warehouseService.createWarehouse(request);
    }

    /**
     * API: PUT /api/warehouses/{id}
     * Cập nhật thông tin tên, địa chỉ và trạng thái của một kho hàng dựa trên ID.
     * Không cho phép thay đổi mã kho để tránh ảnh hưởng dữ liệu nhập/xuất/tồn sau này.
     *
     * @param id      ID của kho hàng cần cập nhật
     * @param request DTO chứa thông tin cập nhật (tenKho, diaChi, trangThai)
     * @return DTO chứa thông tin chi tiết của kho hàng sau khi cập nhật thành công
     */
    @PutMapping("/{id}")
    public WarehouseResponse updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarehouseRequest request
    ) {
        return warehouseService.updateWarehouse(id, request);
    }

    /**
     * API: DELETE /api/warehouses/{id}
     * Ngừng hoạt động một kho hàng dựa trên ID (soft delete).
     * Chỉ thay đổi trạng thái sang NGUNG_HOAT_DONG, không xóa vật lý bản ghi.
     * Ghi chú rõ: không xóa vật lý kho để bảo toàn dữ liệu lịch sử nhập/xuất/tồn.
     *
     * @param id ID của kho hàng cần ngừng hoạt động
     * @return DTO chứa thông tin chi tiết của kho hàng sau khi được ngừng hoạt động
     */
    @DeleteMapping("/{id}")
    public WarehouseResponse deactivateWarehouse(@PathVariable Long id) {
        return warehouseService.deactivateWarehouse(id);
    }
}
