package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.request.CreateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
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
     * Kiểm thử ngoại lệ: Tìm kiếm/lọc kho hàng với trạng thái không hợp lệ.
     */
    @Test
    void getWarehouses_withInvalidStatus_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> 
                warehouseService.getWarehouses(null, "INVALID_STATUS")
        );
    }

    /**
     * Kiểm thử luồng: Thêm mới kho hàng thành công (Happy Path).
     */
    @Test
    void createWarehouse_shouldCreateAndReturnWarehouse() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "KHO003",
                "Kho Đà Nẵng",
                "789 Liên Chiểu",
                "HOAT_DONG"
        );

        Warehouse savedWarehouse = new Warehouse();
        savedWarehouse.setId(3L);
        savedWarehouse.setCode("KHO003");
        savedWarehouse.setName("Kho Đà Nẵng");
        savedWarehouse.setAddress("789 Liên Chiểu");
        savedWarehouse.setStatus(WarehouseStatus.HOAT_DONG);

        Mockito.when(warehouseRepository.existsByCodeIgnoreCase("KHO003")).thenReturn(false);
        Mockito.when(warehouseRepository.saveAndFlush(any(Warehouse.class))).thenReturn(savedWarehouse);

        WarehouseResponse response = warehouseService.createWarehouse(request);

        assertNotNull(response);
        assertEquals(3L, response.id());
        assertEquals("KHO003", response.maKho());
        assertEquals("Kho Đà Nẵng", response.tenKho());
        assertEquals("789 Liên Chiểu", response.diaChi());
        assertEquals("HOAT_DONG", response.trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Thêm mới kho hàng với mã kho đã tồn tại trong hệ thống.
     */
    @Test
    void createWarehouse_withDuplicateCode_shouldThrowFieldValidationException() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "KHO001",
                "Kho Mới Trùng Mã",
                "Địa chỉ ngẫu nhiên",
                "HOAT_DONG"
        );

        Mockito.when(warehouseRepository.existsByCodeIgnoreCase("KHO001")).thenReturn(true);

        FieldValidationException exception = assertThrows(FieldValidationException.class, () -> 
                warehouseService.createWarehouse(request)
        );

        assertTrue(exception.getErrors().containsKey("code"));
        assertEquals("Mã kho đã tồn tại.", exception.getErrors().get("code"));
    }

    /**
     * Kiểm thử luồng: Thêm mới kho hàng khi không truyền trạng thái (mặc định HOAT_DONG).
     */
    @Test
    void createWarehouse_withNullStatus_shouldDefaultToActive() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "KHO004",
                "Kho Cần Thơ",
                "101 Ninh Kiều",
                null
        );

        Warehouse savedWarehouse = new Warehouse();
        savedWarehouse.setId(4L);
        savedWarehouse.setCode("KHO004");
        savedWarehouse.setName("Kho Cần Thơ");
        savedWarehouse.setAddress("101 Ninh Kiều");
        savedWarehouse.setStatus(WarehouseStatus.HOAT_DONG);

        Mockito.when(warehouseRepository.existsByCodeIgnoreCase("KHO004")).thenReturn(false);
        Mockito.when(warehouseRepository.saveAndFlush(any(Warehouse.class))).thenReturn(savedWarehouse);

        WarehouseResponse response = warehouseService.createWarehouse(request);

        assertNotNull(response);
        assertEquals("HOAT_DONG", response.trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Thêm mới kho hàng với trạng thái không hợp lệ.
     */
    @Test
    void createWarehouse_withInvalidStatus_shouldThrowBadRequestException() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "KHO005",
                "Kho Hải Phòng",
                "202 Lê Chân",
                "INVALID_STATUS"
        );

        Mockito.when(warehouseRepository.existsByCodeIgnoreCase("KHO005")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> 
                warehouseService.createWarehouse(request)
        );
    }

    /**
     * Kiểm thử luồng: Cập nhật kho hàng thành công.
     */
    @Test
    void updateWarehouse_shouldUpdateAndReturnWarehouse() {
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "Kho Hà Nội Mới",
                "456 Cầu Giấy Mới",
                "NGUNG_HOAT_DONG"
        );

        Warehouse existingWarehouse = new Warehouse();
        existingWarehouse.setId(1L);
        existingWarehouse.setCode("KHO001");
        existingWarehouse.setName("Kho Hà Nội");
        existingWarehouse.setAddress("123 Cầu Giấy");
        existingWarehouse.setStatus(WarehouseStatus.HOAT_DONG);

        Warehouse updatedWarehouse = new Warehouse();
        updatedWarehouse.setId(1L);
        updatedWarehouse.setCode("KHO001"); // Code must remain unchanged
        updatedWarehouse.setName("Kho Hà Nội Mới");
        updatedWarehouse.setAddress("456 Cầu Giấy Mới");
        updatedWarehouse.setStatus(WarehouseStatus.NGUNG_HOAT_DONG);

        Mockito.when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(existingWarehouse));
        Mockito.when(warehouseRepository.saveAndFlush(any(Warehouse.class))).thenReturn(updatedWarehouse);

        WarehouseResponse response = warehouseService.updateWarehouse(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("KHO001", response.maKho());
        assertEquals("Kho Hà Nội Mới", response.tenKho());
        assertEquals("456 Cầu Giấy Mới", response.diaChi());
        assertEquals("NGUNG_HOAT_DONG", response.trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Cập nhật kho hàng với ID không tồn tại.
     */
    @Test
    void updateWarehouse_withNonExistingId_shouldThrowNotFoundException() {
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "Kho Cập Nhật",
                "Địa chỉ mới",
                "HOAT_DONG"
        );

        Mockito.when(warehouseRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () ->
                warehouseService.updateWarehouse(99L, request)
        );
    }

    /**
     * Kiểm thử ngoại lệ: Cập nhật kho hàng với trạng thái không hợp lệ.
     */
    @Test
    void updateWarehouse_withInvalidStatus_shouldThrowBadRequestException() {
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "Kho Cập Nhật",
                "Địa chỉ mới",
                "INVALID_STATUS"
        );

        Warehouse existingWarehouse = new Warehouse();
        existingWarehouse.setId(1L);
        existingWarehouse.setCode("KHO001");
        existingWarehouse.setName("Kho Hà Nội");
        existingWarehouse.setAddress("123 Cầu Giấy");
        existingWarehouse.setStatus(WarehouseStatus.HOAT_DONG);

        Mockito.when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(existingWarehouse));

        assertThrows(BadRequestException.class, () ->
                warehouseService.updateWarehouse(1L, request)
        );
    }

    /**
     * Kiểm thử luồng: Ngừng hoạt động kho hàng thành công.
     */
    @Test
    void deactivateWarehouse_shouldDeactivateAndReturnWarehouse() {
        Warehouse existingWarehouse = new Warehouse();
        existingWarehouse.setId(1L);
        existingWarehouse.setCode("KHO001");
        existingWarehouse.setName("Kho Hà Nội");
        existingWarehouse.setAddress("123 Cầu Giấy");
        existingWarehouse.setStatus(WarehouseStatus.HOAT_DONG);

        Warehouse deactivatedWarehouse = new Warehouse();
        deactivatedWarehouse.setId(1L);
        deactivatedWarehouse.setCode("KHO001");
        deactivatedWarehouse.setName("Kho Hà Nội");
        deactivatedWarehouse.setAddress("123 Cầu Giấy");
        deactivatedWarehouse.setStatus(WarehouseStatus.NGUNG_HOAT_DONG);

        Mockito.when(warehouseRepository.findById(1L)).thenReturn(java.util.Optional.of(existingWarehouse));
        Mockito.when(warehouseRepository.saveAndFlush(any(Warehouse.class))).thenReturn(deactivatedWarehouse);

        WarehouseResponse response = warehouseService.deactivateWarehouse(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("NGUNG_HOAT_DONG", response.trangThai());
    }

    /**
     * Kiểm thử ngoại lệ: Ngừng hoạt động kho hàng với ID không tồn tại.
     */
    @Test
    void deactivateWarehouse_withNonExistingId_shouldThrowNotFoundException() {
        Mockito.when(warehouseRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () ->
                warehouseService.deactivateWarehouse(99L)
        );
    }
}
