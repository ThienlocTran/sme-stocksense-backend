package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;
import com.smartflow.smestocksensebackend.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller tiếp nhận các yêu cầu HTTP bên ngoài liên quan đến quản lý Đối Tác.
 */
@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * API: GET /api/partners
     * Trả về danh sách đối tác dựa trên từ khóa tìm kiếm động, loại đối tác và trạng thái hoạt động.
     * Quyền truy cập: Admin/IT (ADMIN), Quản lý kho (MANAGER) và Nhân viên thủ kho (EMPLOYEE) được phép xem.
     *
     * @param keyword    Từ khóa tìm kiếm tùy chọn (mã, tên, số điện thoại, email hoặc người liên hệ)
     * @param loaiDoiTac Loại đối tác lọc tùy chọn (NHA_CUNG_CAP, KHACH_HANG, CA_HAI)
     * @param trangThai  Trạng thái hoạt động lọc tùy chọn (HOAT_DONG, NGUNG_HOAT_DONG)
     */
    @GetMapping
    public List<PartnerResponse> getPartners(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiDoiTac,
            @RequestParam(required = false) String trangThai
    ) {
        return partnerService.getPartners(keyword, loaiDoiTac, trangThai);
    }
}
