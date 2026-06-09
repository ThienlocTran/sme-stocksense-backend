package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
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
}
