package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.request.CreatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit Test kiểm thử logic nghiệp vụ của PartnerServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class PartnerServiceImplTest {

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private PartnerServiceImpl partnerService;

    private Partner partner1;
    private Partner partner2;

    /**
     * Dựng dữ liệu giả lập sạch trước mỗi ca kiểm thử.
     */
    @BeforeEach
    void setUp() {
        partner1 = new Partner();
        partner1.setId(1L);
        partner1.setCode("NCC001");
        partner1.setName("Công ty cung cấp A");
        partner1.setType(PartnerType.NHA_CUNG_CAP);
        partner1.setContactPerson("Nguyễn Văn A");
        partner1.setPhoneNumber("0912345678");
        partner1.setEmail("ncc_a@example.com");
        partner1.setAddress("Hà Nội");
        partner1.setStatus(PartnerStatus.HOAT_DONG);

        partner2 = new Partner();
        partner2.setId(2L);
        partner2.setCode("KH001");
        partner2.setName("Khách hàng B");
        partner2.setType(PartnerType.KHACH_HANG);
        partner2.setContactPerson("Trần Thị B");
        partner2.setPhoneNumber("0987654321");
        partner2.setEmail("kh_b@example.com");
        partner2.setAddress("Hồ Chí Minh");
        partner2.setStatus(PartnerStatus.NGUNG_HOAT_DONG);
    }

    /**
     * Kiểm thử luồng: Lấy toàn bộ danh sách đối tác thành công.
     */
    @Test
    void getPartners_shouldReturnAllPartners() {
        Mockito.when(partnerRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(partner1, partner2));

        List<PartnerResponse> result = partnerService.getPartners(null, null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("NCC001", result.get(0).maDoiTac());
        assertEquals("NHA_CUNG_CAP", result.get(0).loaiDoiTac());
        assertEquals("HOAT_DONG", result.get(0).trangThai());
        assertEquals("KH001", result.get(1).maDoiTac());
        assertEquals("KHACH_HANG", result.get(1).loaiDoiTac());
        assertEquals("NGUNG_HOAT_DONG", result.get(1).trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Lọc đối tác với trạng thái hoạt động không hợp lệ.
     */
    @Test
    void getPartners_withInvalidStatus_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () ->
                partnerService.getPartners(null, null, "INVALID_STATUS")
        );
    }

    /**
     * Kiểm thử ngoại lệ: Lọc đối tác với loại đối tác không hợp lệ.
     */
    @Test
    void getPartners_withInvalidType_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () ->
                partnerService.getPartners(null, "INVALID_TYPE", null)
        );
    }

    /**
     * Kiểm thử luồng: Tạo mới đối tác thành công.
     */
    @Test
    void createPartner_shouldCreateAndReturnPartner() {
        CreatePartnerRequest request = new CreatePartnerRequest(
                "NCC003",
                "Công ty Gia Phát",
                "NHA_CUNG_CAP",
                "Nguyễn Văn A",
                "0901234567",
                "contact@example.com",
                "TP.HCM",
                "HOAT_DONG"
        );

        Mockito.when(partnerRepository.existsByCodeIgnoreCase("NCC003")).thenReturn(false);
        Mockito.when(partnerRepository.saveAndFlush(any(Partner.class))).thenAnswer(invocation -> {
            Partner p = invocation.getArgument(0);
            p.setId(3L);
            return p;
        });

        PartnerResponse result = partnerService.createPartner(request);

        assertNotNull(result);
        assertEquals(3L, result.id());
        assertEquals("NCC003", result.maDoiTac());
        assertEquals("Công ty Gia Phát", result.tenDoiTac());
        assertEquals("NHA_CUNG_CAP", result.loaiDoiTac());
        assertEquals("HOAT_DONG", result.trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Tạo mới đối tác với mã đã tồn tại.
     */
    @Test
    void createPartner_withDuplicateCode_shouldThrowFieldValidationException() {
        CreatePartnerRequest request = new CreatePartnerRequest(
                "NCC001",
                "Đối tác trùng mã",
                "NHA_CUNG_CAP",
                null, null, null, null, null
        );

        Mockito.when(partnerRepository.existsByCodeIgnoreCase("NCC001")).thenReturn(true);

        assertThrows(FieldValidationException.class, () -> partnerService.createPartner(request));
    }

    /**
     * Kiểm thử luồng: Cập nhật đối tác thành công.
     */
    @Test
    void updatePartner_shouldUpdateAndReturnPartner() {
        UpdatePartnerRequest request = new UpdatePartnerRequest(
                "Công ty Gia Phát cập nhật",
                "CA_HAI",
                "Nguyễn Văn B",
                "0909999999",
                "update@example.com",
                "Bình Dương",
                "NGUNG_HOAT_DONG"
        );

        Mockito.when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner1));
        Mockito.when(partnerRepository.saveAndFlush(any(Partner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerResponse result = partnerService.updatePartner(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("NCC001", result.maDoiTac()); // Mã đối tác không thay đổi
        assertEquals("Công ty Gia Phát cập nhật", result.tenDoiTac());
        assertEquals("CA_HAI", result.loaiDoiTac());
        assertEquals("NGUNG_HOAT_DONG", result.trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Cập nhật đối tác với ID không tồn tại.
     */
    @Test
    void updatePartner_withNonExistentId_shouldThrowNotFoundException() {
        UpdatePartnerRequest request = new UpdatePartnerRequest(
                "Công ty Cổ phần X", "NHA_CUNG_CAP", null, null, null, null, "HOAT_DONG"
        );

        Mockito.when(partnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> partnerService.updatePartner(99L, request));
    }
}
