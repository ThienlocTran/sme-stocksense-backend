package com.smartflow.smestocksensebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) chứa dữ liệu yêu cầu tạo mới kho hàng từ Client gửi lên.
 * Định nghĩa các quy tắc xác thực dữ liệu đầu vào cơ bản cho các trường thông tin.
 */
public record CreateWarehouseRequest(
        // Mã kho hàng là định danh nghiệp vụ nên không được trùng, bắt buộc nhập, tối đa 50 ký tự
        @NotBlank(message = "Mã kho không được để trống.")
        @Size(max = 50, message = "Mã kho không được vượt quá 50 ký tự.")
        String maKho,

        // Tên kho hàng: bắt buộc nhập, tối đa 150 ký tự
        @NotBlank(message = "Tên kho không được để trống.")
        @Size(max = 150, message = "Tên kho không được vượt quá 150 ký tự.")
        String tenKho,

        // Địa chỉ kho hàng: tùy chọn, tối đa 255 ký tự
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
        String diaChi,

        // Trạng thái kho hàng: tùy chọn, mặc định là HOAT_DONG ở tầng Service nếu không truyền
        String trangThai,

        java.math.BigDecimal maxCapacityM3
) {
    public CreateWarehouseRequest(String maKho, String tenKho, String diaChi, String trangThai) {
        this(maKho, tenKho, diaChi, trangThai, null);
    }
}
