package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller tiếp nhận các yêu cầu HTTP bên ngoài liên quan đến Kho Hàng.
 */
@RestController // Đánh dấu đây là REST Controller trả về dữ liệu trực tiếp dưới dạng JSON
@RequestMapping("/api/warehouses") // Định nghĩa tiền tố đường dẫn API cho tất cả tài nguyên kho hàng
@RequiredArgsConstructor // Tự sinh constructor chứa tham số final để Spring thực hiện cơ chế dependency
                         // injection
public class WarehouseController {

    private final WarehouseService warehouseService; // Inject lớp tầng nghiệp vụ WarehouseService

    /**
     * API Endpoint: GET /api/warehouses
     * Trả về danh sách các kho hàng theo điều kiện tìm kiếm động và lọc trạng thái
     * hoạt động.
     * 
     * @param keyword Từ khóa tìm kiếm tùy chọn gửi từ client (tìm kiếm theo mã,
     *                tên, địa chỉ kho)
     * @param status  Trạng thái lọc tùy chọn gửi từ client (chỉ chấp nhận ACTIVE
     *                hoặc INACTIVE)
     * @return Danh sách DTO WarehouseResponse chứa thông tin kho
     */
    @GetMapping // Map các yêu cầu HTTP GET tại endpoint /api/warehouses vào phương thức này
    public List<WarehouseResponse> getWarehouses(
            @RequestParam(required = false) String keyword, // Ánh xạ tham số truy vấn tùy chọn '?keyword=...'
            @RequestParam(required = false) String status // Ánh xạ tham số truy vấn tùy chọn '?status=...'
    ) {
        // Chuyển hướng yêu cầu tới tầng Service xử lý nghiệp vụ tìm kiếm/lọc
        return warehouseService.getWarehouses(keyword, status);
    }
}
