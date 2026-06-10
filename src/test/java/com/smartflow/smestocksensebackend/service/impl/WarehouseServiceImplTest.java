package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
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
 * Unit Test kiểm thử logic nghiệp vụ của WarehouseServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse warehouse1;
    private Warehouse warehouse2;

    /**
     * Dựng dữ liệu giả lập sạch trước mỗi ca kiểm thử.
     */
    @BeforeEach
    void setUp() {
        warehouse1 = new Warehouse();
        warehouse1.setId(1L);
        warehouse1.setCode("KHO001");
        warehouse1.setName("Kho Hà Nội");
        warehouse1.setAddress("123 Cầu Giấy");
        warehouse1.setStatus(WarehouseStatus.HOAT_DONG);

        warehouse2 = new Warehouse();
        warehouse2.setId(2L);
        warehouse2.setCode("KHO002");
        warehouse2.setName("Kho Sài Gòn");
        warehouse2.setAddress("456 Quận 1");
        warehouse2.setStatus(WarehouseStatus.NGUNG_HOAT_DONG);
    }

    /**
     * Kiểm thử luồng: Lấy toàn bộ danh sách kho hàng thành công.
     */
    @Test
    void getWarehouses_shouldReturnAllWarehouses() {
        Mockito.when(warehouseRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(warehouse1, warehouse2));

        List<WarehouseResponse> result = warehouseService.getWarehouses(null, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("KHO001", result.get(0).maKho());
        assertEquals("HOAT_DONG", result.get(0).trangThai());
        assertEquals("KHO002", result.get(1).maKho());
        assertEquals("NGUNG_HOAT_DONG", result.get(1).trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Lọc kho hàng với trạng thái không hợp lệ phải ném BadRequestException.
     */
    @Test
    void getWarehouses_withInvalidStatus_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () ->
                warehouseService.getWarehouses(null, "INVALID_STATUS")
        );
    }
}
