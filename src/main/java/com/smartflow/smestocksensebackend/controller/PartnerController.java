package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.request.CreatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.response.PartnerDropdownResponse;
import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;
import com.smartflow.smestocksensebackend.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @GetMapping("/dropdown/suppliers")
    public List<PartnerDropdownResponse> getSupplierDropdown() {
        return partnerService.getActiveSuppliers();
    }

    @GetMapping
    public List<PartnerResponse> getPartners(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiDoiTac,
            @RequestParam(required = false) String trangThai
    ) {
        return partnerService.getPartners(keyword, loaiDoiTac, trangThai);
    }

    /**
     * API: POST /api/partners
     * Tiếp nhận yêu cầu thêm mới một đối tác và thực hiện các xác thực dữ liệu đầu vào.
     * Quyền truy cập: Admin/IT (ADMIN) và Quản lý kho (MANAGER) được phép tạo mới.
     *
     * @param request DTO chứa thông tin đối tác cần tạo mới
     * @return DTO chứa thông tin chi tiết của đối tác vừa được tạo thành công
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse createPartner(@Valid @RequestBody CreatePartnerRequest request) {
        return partnerService.createPartner(request);
    }

    /**
     * API: PUT /api/partners/{id}
     * Cập nhật thông tin chi tiết đối tác dựa trên ID.
     * Quyền truy cập: Admin/IT (ADMIN) và Quản lý kho (MANAGER) được phép cập nhật.
     *
     * @param id      ID đối tác cần cập nhật
     * @param request DTO chứa thông tin cập nhật (tenDoiTac, loaiDoiTac, nguoiLienHe, soDienThoai, email, diaChi, trangThai)
     * @return DTO chứa thông tin đối tác sau khi cập nhật thành công
     */
    @PutMapping("/{id}")
    public PartnerResponse updatePartner(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePartnerRequest request
    ) {
        return partnerService.updatePartner(id, request);
    }
}
