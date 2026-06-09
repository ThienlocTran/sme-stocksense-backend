package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;

import java.util.List;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ (Business Logic) liên quan đến quản lý Đối Tác.
 */
public interface PartnerService {

    /**
     * Lấy danh sách đối tác có hỗ trợ tìm kiếm động và lọc theo loại đối tác, trạng thái hoạt động.
     *
     * @param keyword     Từ khóa tìm kiếm tùy chọn (mã, tên, số điện thoại, email hoặc người liên hệ)
     * @param loaiDoiTac  Loại đối tác lọc tùy chọn (NHA_CUNG_CAP, KHACH_HANG, CA_HAI)
     * @param trangThai   Trạng thái hoạt động lọc tùy chọn (HOAT_DONG, NGUNG_HOAT_DONG)
     * @return Danh sách DTO PartnerResponse đại diện cho các đối tác phù hợp điều kiện lọc
     */
    List<PartnerResponse> getPartners(String keyword, String loaiDoiTac, String trangThai);
}
