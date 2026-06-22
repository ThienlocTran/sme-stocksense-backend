package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit Test kiểm thử logic nghiệp vụ của InventoryServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product product;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setCode("SP001");
        product.setName("Sản phẩm 1");

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("KHO001");
        warehouse.setName("Kho 1");
    }

    /**
     * Kiểm thử luồng: Sản phẩm chưa có tồn kho tại kho này.
     * Kỳ vọng: Tạo mới bản ghi tồn kho (Insert) với số lượng bằng số truyền vào.
     */
    @Test
    void increaseInventory_success_whenInventoryDoesNotExist() {
        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        Mockito.when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        inventoryService.increaseInventory(1L, 1L, 50);

        ArgumentCaptor<InventoryLevel> inventoryCaptor = ArgumentCaptor.forClass(InventoryLevel.class);
        Mockito.verify(inventoryLevelRepository).saveAndFlush(inventoryCaptor.capture());

        InventoryLevel savedInventory = inventoryCaptor.getValue();
        assertNotNull(savedInventory);
        assertEquals(product, savedInventory.getProduct());
        assertEquals(warehouse, savedInventory.getWarehouse());
        assertEquals(50, savedInventory.getQuantity());
    }

    /**
     * Kiểm thử luồng: Sản phẩm đã có tồn kho tại kho này.
     * Kỳ vọng: Cộng dồn số lượng thực nhận vào số lượng tồn hiện tại (Update).
     */
    @Test
    void increaseInventory_success_whenInventoryExists() {
        InventoryLevel existingInventory = new InventoryLevel();
        existingInventory.setId(10L);
        existingInventory.setProduct(product);
        existingInventory.setWarehouse(warehouse);
        existingInventory.setQuantity(100);

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        Mockito.when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(existingInventory));

        inventoryService.increaseInventory(1L, 1L, 50);

        ArgumentCaptor<InventoryLevel> inventoryCaptor = ArgumentCaptor.forClass(InventoryLevel.class);
        Mockito.verify(inventoryLevelRepository).saveAndFlush(inventoryCaptor.capture());

        InventoryLevel savedInventory = inventoryCaptor.getValue();
        assertNotNull(savedInventory);
        assertEquals(10L, savedInventory.getId());
        assertEquals(150, savedInventory.getQuantity());
    }

    /**
     * Kiểm thử ngoại lệ: Không tìm thấy sản phẩm.
     * Kỳ vọng: Ném NotFoundException.
     */
    @Test
    void increaseInventory_error_whenProductNotFound() {
        Mockito.when(productRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                inventoryService.increaseInventory(99L, 1L, 50)
        );

        assertEquals("Sản phẩm không tồn tại.", exception.getMessage());
        Mockito.verify(inventoryLevelRepository, Mockito.never()).saveAndFlush(any());
    }

    /**
     * Kiểm thử ngoại lệ: Không tìm thấy kho hàng.
     * Kỳ vọng: Ném NotFoundException.
     */
    @Test
    void increaseInventory_error_whenWarehouseNotFound() {
        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                inventoryService.increaseInventory(1L, 99L, 50)
        );

        assertEquals("Kho hàng không tồn tại.", exception.getMessage());
        Mockito.verify(inventoryLevelRepository, Mockito.never()).saveAndFlush(any());
    }

    /**
     * Kiểm thử ngoại lệ: Tham số đầu vào không hợp lệ (quantity = 0).
     * Kỳ vọng: Ném IllegalArgumentException với message đúng trước khi truy vấn DB.
     */
    @Test
    void increaseInventory_error_whenInvalidInput() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.increaseInventory(1L, 1L, 0)
        );

        assertEquals("productId, warehouseId, quantity phải > 0", exception.getMessage());
        Mockito.verify(inventoryLevelRepository, Mockito.never()).saveAndFlush(any());
    }

    /**
     * Kiểm thử ngoại lệ: productId = 0 (không hợp lệ vì phải > 0).
     * Kỳ vọng: Ném IllegalArgumentException ngay trước khi truy vấn DB.
     */
    @Test
    void increaseInventory_error_whenProductIdInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.increaseInventory(0L, 1L, 50)
        );

        assertEquals("productId, warehouseId, quantity phải > 0", exception.getMessage());
        Mockito.verify(inventoryLevelRepository, Mockito.never()).saveAndFlush(any());
    }

    /**
     * Kiểm thử ngoại lệ: warehouseId = -1 (âm, không hợp lệ vì phải > 0).
     * Kỳ vọng: Ném IllegalArgumentException ngay trước khi truy vấn DB.
     */
    @Test
    void increaseInventory_error_whenWarehouseIdInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.increaseInventory(1L, -1L, 50)
        );

        assertEquals("productId, warehouseId, quantity phải > 0", exception.getMessage());
        Mockito.verify(inventoryLevelRepository, Mockito.never()).saveAndFlush(any());
    }

    /**
     * Kiểm thử ngoại lệ: Tổng số lượng tồn kho vượt Integer.MAX_VALUE (Integer Overflow).
     * Kỳ vọng: Ném IllegalArgumentException với message đúng, không gọi saveAndFlush.
     */
    @Test
    void increaseInventory_error_whenQuantityOverflow() {
        InventoryLevel existingInventory = new InventoryLevel();
        existingInventory.setId(10L);
        existingInventory.setProduct(product);
        existingInventory.setWarehouse(warehouse);
        existingInventory.setQuantity(Integer.MAX_VALUE);

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        Mockito.when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(existingInventory));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.increaseInventory(1L, 1L, 1)
        );

        assertEquals("Tong so luong ton kho vuot gioi han cho phep.", ex.getMessage());
        Mockito.verify(inventoryLevelRepository, Mockito.never()).saveAndFlush(any());
    }

    /**
     * Kiểm thử race condition: lần insert đầu bị DataIntegrityViolationException
     * (transaction khác đã insert trước). Kỳ vọng: fallback sang update, cộng dồn đúng.
     */
    @Test
    void increaseInventory_success_whenInsertRacesAndFallbackToUpdate() {
        InventoryLevel lockedInventory = new InventoryLevel();
        lockedInventory.setId(10L);
        lockedInventory.setProduct(product);
        lockedInventory.setWarehouse(warehouse);
        lockedInventory.setQuantity(100);

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        // Lần 1: check trước insert -> empty; Lần 2: trong catch -> trả bản ghi bị khóa
        Mockito.when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(lockedInventory));

        // Lần 1: ném DataIntegrityViolationException; Lần 2: trả về đối tượng được lưu
        Mockito.when(inventoryLevelRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.increaseInventory(1L, 1L, 50);

        ArgumentCaptor<InventoryLevel> captor = ArgumentCaptor.forClass(InventoryLevel.class);
        Mockito.verify(inventoryLevelRepository, Mockito.times(2)).saveAndFlush(captor.capture());

        List<InventoryLevel> allSaves = captor.getAllValues();
        InventoryLevel secondSave = allSaves.get(1);
        assertNotNull(secondSave);
        assertEquals(150, secondSave.getQuantity());
    }

    /**
     * Kiểm thử race condition cực biên: lần insert bị DataIntegrityViolationException
     * nhưng lần fallback query (trong catch) vẫn trả về empty (bản ghi đã bị xóa hoặc lỗi DB).
     * Kỳ vọng: exception gốc được ném lại nguyên vẹn (assertSame), saveAndFlush chỉ gọi đúng 1 lần.
     */
    @Test
    void increaseInventory_error_whenInsertRacesAndFallbackMissingRecord() {
        DataIntegrityViolationException expectedException =
                new DataIntegrityViolationException("duplicate key – fallback empty");

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        // Cả lần 1 (trước insert) và lần 2 (trong catch) đều trả empty
        Mockito.when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());

        // saveAndFlush ném exception gốc
        Mockito.when(inventoryLevelRepository.saveAndFlush(any()))
                .thenThrow(expectedException);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> inventoryService.increaseInventory(1L, 1L, 50)
        );

        // Exception phải là chính xác instance đã mock, không phải exception mới
        assertSame(expectedException, thrown);

        // saveAndFlush chỉ được gọi đúng 1 lần (không có lần 2 vì fallback đã throw)
        Mockito.verify(inventoryLevelRepository, Mockito.times(1)).saveAndFlush(any());
    }

    /**
     * Kiểm thử luồng: Tăng tồn kho kèm phiếu nhập.
     * Kỳ vọng: Tăng tồn kho thành công và gọi service ghi log giao dịch kho loại NHAP_KHO.
     */
    @Test
    void increaseInventory_withImportReceipt_shouldLogTransaction() {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(5L);
        receipt.setStatus(ImportReceiptStatus.HOAN_THANH);

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        Mockito.when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        inventoryService.increaseInventory(1L, 1L, 50, receipt);

        ArgumentCaptor<InventoryLevel> inventoryCaptor = ArgumentCaptor.forClass(InventoryLevel.class);
        Mockito.verify(inventoryLevelRepository).saveAndFlush(inventoryCaptor.capture());

        InventoryLevel savedInventory = inventoryCaptor.getValue();
        assertNotNull(savedInventory);
        assertEquals(50, savedInventory.getQuantity());

        Mockito.verify(inventoryTransactionService).recordTransaction(
                1L,
                1L,
                InventoryTransactionType.NHAP_KHO,
                50,
                0,
                50,
                receipt,
                "Kế thừa T73, track biến động kho phục vụ đối soát"
        );
    }
}
