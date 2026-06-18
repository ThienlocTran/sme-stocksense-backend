package com.smartflow.smestocksensebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) chứa dữ liệu yêu cầu cập nhật thông tin kho hàng từ Client gửi lên.
 * Định nghĩa các quy tắc xác thực dữ liệu đầu vào cơ bản cho các trường thông tin được phép sửa.
 */
public record UpdateWarehouseRequest(
        // Tên kho hàng: bắt buộc nhập, tối đa 150 ký tự
        @NotBlank(message = "Tên kho không được để trống.")
        @Size(max = 150, message = "Tên kho không được vượt quá 150 ký tự.")
        String tenKho,

        // Địa chỉ kho hàng: tùy chọn, tối đa 255 ký tự
        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
        String diaChi,

        // Trạng thái kho hàng: bắt buộc, chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG
        @NotBlank(message = "Trạng thái không được để trống.")
        @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG", message = "Trạng thái chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.")
        String trangThai
) {
}
