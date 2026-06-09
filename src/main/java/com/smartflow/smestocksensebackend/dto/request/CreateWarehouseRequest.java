package com.smartflow.smestocksensebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) chứa dữ liệu yêu cầu tạo mới kho hàng từ Client gửi lên.
 * Định nghĩa các quy tắc xác thực dữ liệu đầu vào cơ bản cho các trường thông tin.
 */
public record CreateWarehouseRequest(
        // Mã kho hàng: bắt buộc nhập, không trống, độ dài tối đa 50 ký tự
        @NotBlank(message = "Mã kho không được để trống.")
        @Size(max = 50, message = "Mã kho không được vượt quá 50 ký tự.")
        String code,

        // Tên kho hàng: bắt buộc nhập, không trống, độ dài tối đa 150 ký tự
        @NotBlank(message = "Tên kho không được để trống.")
        @Size(max = 150, message = "Tên kho không được vượt quá 150 ký tự.")
        String name,

        // Địa chỉ kho hàng: tùy chọn, độ dài tối đa 255 ký tự
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
        String address,

        // Trạng thái kho hàng: tùy chọn, nếu không truyền sẽ mặc định là ACTIVE ở tầng Service
        String status
) {
}
