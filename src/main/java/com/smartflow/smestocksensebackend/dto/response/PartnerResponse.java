package com.smartflow.smestocksensebackend.dto.response;

import com.smartflow.smestocksensebackend.entity.Partner;

/**
 * DTO trả về thông tin đối tác cho Client.
 * Tên field dùng camelCase tương ứng với tên cột tiếng Việt không dấu trong DB.
 * Ghi chú nghiệp vụ: loaiDoiTac dùng để phân biệt nhà cung cấp (NHA_CUNG_CAP), khách hàng (KHACH_HANG) hoặc cả hai (CA_HAI).
 */
public record PartnerResponse(
        Long id,
        String maDoiTac,
        String tenDoiTac,
        String loaiDoiTac,
        String nguoiLienHe,
        String soDienThoai,
        String email,
        String diaChi,
        String trangThai
) {
    /**
     * Chuyển đổi từ Entity Partner sang DTO PartnerResponse.
     */
    public static PartnerResponse from(Partner partner) {
        return new PartnerResponse(
                partner.getId(),
                partner.getCode(),
                partner.getName(),
                partner.getType().name(),
                partner.getContactPerson(),
                partner.getPhoneNumber(),
                partner.getEmail(),
                partner.getAddress(),
                partner.getStatus().name()
        );
    }
}
