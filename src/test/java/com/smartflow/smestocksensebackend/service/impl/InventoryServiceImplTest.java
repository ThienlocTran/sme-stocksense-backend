package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
