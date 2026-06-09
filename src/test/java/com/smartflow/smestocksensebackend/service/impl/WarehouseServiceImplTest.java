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
 * Lớp Unit Test kiểm thử logic nghiệp vụ của WarehouseServiceImpl bằng JUnit 5 và Mockito.
 */
@ExtendWith(MockitoExtension.class) // Tích hợp Mockito Extension tự động khởi tạo và giải phóng các Mock
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository; // Tạo một đối tượng giả lập (mock) cho repository tương tác DB

    @InjectMocks
    private WarehouseServiceImpl warehouseService; // Inject đối tượng giả lập ở trên vào lớp Service thực tế cần test

    private Warehouse warehouse1; // Khai báo đối tượng kho mẫu 1 phục vụ test
    private Warehouse warehouse2; // Khai báo đối tượng kho mẫu 2 phục vụ test

    /**
     * Phương thức chạy trước mỗi test case để chuẩn bị sẵn dữ liệu giả lập sạch.
     */
    @BeforeEach
    void setUp() {
        // Thiết lập kho mẫu 1 có trạng thái hoạt động (ACTIVE)
        warehouse1 = new Warehouse();
        warehouse1.setId(1L);
        warehouse1.setCode("WH001");
        warehouse1.setName("Kho Hà Nội");
        warehouse1.setAddress("123 Cầu Giấy");
        warehouse1.setStatus(WarehouseStatus.ACTIVE);

        // Thiết lập kho mẫu 2 có trạng thái ngừng hoạt động (INACTIVE)
        warehouse2 = new Warehouse();
        warehouse2.setId(2L);
        warehouse2.setCode("WH002");
        warehouse2.setName("Kho Sài Gòn");
        warehouse2.setAddress("456 Quận 1");
        warehouse2.setStatus(WarehouseStatus.INACTIVE);
    }

    /**
     * Kiểm thử luồng thành công: Lấy toàn bộ danh sách kho hàng và chuyển đổi sang định dạng DTO chính xác.
     */
    @Test
    void getWarehouses_shouldReturnAllWarehouses() {
        // Giả lập hành vi: khi gọi repository.findAll với bất kỳ Specification nào, trả về danh sách 2 kho mẫu đã dựng sẵn
        Mockito.when(warehouseRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(warehouse1, warehouse2));

        // Gọi phương thức getWarehouses của lớp Service để thực hiện kiểm thử thực tế
        List<WarehouseResponse> result = warehouseService.getWarehouses(null, null);

        // Khẳng định dữ liệu trả về đúng kỳ vọng
        assertNotNull(result); // Đảm bảo danh sách trả về không bị null
        assertEquals(2, result.size()); // Kiểm tra số lượng phần tử trả về phải bằng 2
        assertEquals("WH001", result.get(0).code()); // Kiểm tra mã kho đầu tiên đúng là WH001
        assertEquals("ACTIVE", result.get(0).status()); // Kiểm tra trạng thái kho đầu tiên đúng là ACTIVE
        assertEquals("WH002", result.get(1).code()); // Kiểm tra mã kho thứ hai đúng là WH002
        assertEquals("INACTIVE", result.get(1).status()); // Kiểm tra trạng thái kho thứ hai đúng là INACTIVE
    }

    /**
     * Kiểm thử luồng ngoại lệ: Khi người dùng lọc theo một trạng thái không hợp lệ (không phải ACTIVE/INACTIVE),
     * phương thức phải ném lỗi BadRequestException để Controller chuyển thành HTTP Status 400.
     */
    @Test
    void getWarehouses_withInvalidStatus_shouldThrowBadRequestException() {
        // Khẳng định rằng phương thức sẽ ném ra ngoại lệ BadRequestException khi truyền trạng thái không đúng
        assertThrows(BadRequestException.class, () -> 
                warehouseService.getWarehouses(null, "INVALID_STATUS")
        );
    }
}
