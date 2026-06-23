package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class InventoryTransactionServiceImplTest {

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private InventoryTransactionServiceImpl inventoryTransactionService;

    private Product product;
    private Warehouse warehouse;
    private Employee employee;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setCode("SP001");

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("KHO001");

        employee = new Employee();
        employee.setId(10L);
        employee.setFullName("Nguyễn Văn A");
    }

    @Test
    void recordTransaction_success_withCreator() {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(5L);

        // Thiết lập SecurityContext giả lập đăng nhập
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.isAuthenticated()).thenReturn(true);
        Mockito.when(authentication.getPrincipal()).thenReturn(employee);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        Mockito.when(inventoryTransactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransaction transaction = inventoryTransactionService.recordTransaction(
                1L,
                1L,
                InventoryTransactionType.NHAP_KHO,
                50,
                100,
                150,
                receipt,
                "Kế thừa T73, track biến động kho phục vụ đối soát"
        );

        assertNotNull(transaction);
        assertEquals(product, transaction.getProduct());
        assertEquals(warehouse, transaction.getWarehouse());
        assertEquals(InventoryTransactionType.NHAP_KHO, transaction.getTransactionType());
        assertEquals(50, transaction.getQuantity());
        assertEquals(100, transaction.getQuantityBefore());
        assertEquals(150, transaction.getQuantityAfter());
        assertEquals(receipt, transaction.getImportReceipt());
        assertEquals("Kế thừa T73, track biến động kho phục vụ đối soát", transaction.getNote());
        assertEquals(employee, transaction.getCreatedBy());

        SecurityContextHolder.clearContext();
    }

    @Test
    void recordTransaction_success_withoutCreator() {
        SecurityContextHolder.clearContext();

        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        Mockito.when(inventoryTransactionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryTransaction transaction = inventoryTransactionService.recordTransaction(
                1L,
                1L,
                InventoryTransactionType.NHAP_KHO,
                50,
                100,
                150,
                null,
                "No receipt"
        );

        assertNotNull(transaction);
        assertEquals(product, transaction.getProduct());
        assertEquals(warehouse, transaction.getWarehouse());
        assertNull(transaction.getImportReceipt());
        assertNull(transaction.getCreatedBy());
    }

    @Test
    void recordTransaction_error_whenProductNotFound() {
        Mockito.when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                inventoryTransactionService.recordTransaction(
                        99L,
                        1L,
                        InventoryTransactionType.NHAP_KHO,
                        50,
                        100,
                        150,
                        null,
                        "Test"
                )
        );
    }

    @Test
    void recordTransaction_error_whenWarehouseNotFound() {
        Mockito.when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        Mockito.when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                inventoryTransactionService.recordTransaction(
                        1L,
                        99L,
                        InventoryTransactionType.NHAP_KHO,
                        50,
                        100,
                        150,
                        null,
                        "Test"
                )
        );
    }
}
