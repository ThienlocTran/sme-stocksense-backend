package com.smartflow.smestocksensebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO chứa yêu cầu tạo mới đối tác từ Client gửi lên.
 * Ghi chú nghiệp vụ: loaiDoiTac chỉ nhận: NHA_CUNG_CAP, KHACH_HANG, CA_HAI để tránh sai lệch luồng nghiệp vụ kho.
 */
public record CreatePartnerRequest(
        // Mã đối tác: tùy chọn khi gửi từ client, nếu không gửi hệ thống tự động phát sinh
        @Size(max = 50, message = "Mã đối tác không được vượt quá 50 ký tự.")
        String maDoiTac,

        @NotBlank(message = "Tên đối tác không được để trống.")
        @Size(max = 150, message = "Tên đối tác không được vượt quá 150 ký tự.")
        String tenDoiTac,

        @NotBlank(message = "Loại đối tác không được để trống.")
        @Pattern(regexp = "NHA_CUNG_CAP|KHACH_HANG|CA_HAI", message = "Loại đối tác chỉ nhận NHA_CUNG_CAP, KHACH_HANG hoặc CA_HAI.")
        String loaiDoiTac,

        @Size(max = 150, message = "Tên người liên hệ không được vượt quá 150 ký tự.")
        String nguoiLienHe,

        @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
        String soDienThoai,

        // Validate email rỗng hoặc đúng định dạng email
        @Pattern(regexp = "^$|^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "Email không đúng định dạng.")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
        String email,

        @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự.")
        String diaChi,

        // Trạng thái đối tác: mặc định là HOAT_DONG nếu không truyền
        @Pattern(regexp = "HOAT_DONG|NGUNG_HOAT_DONG", message = "Trạng thái chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.")
        String trangThai
) {}
