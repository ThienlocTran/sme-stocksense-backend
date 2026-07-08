package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.RejectExportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ExportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportReceiptApprovalServiceTest {

    @Mock
    private ExportReceiptRepository exportReceiptRepository;

    @Mock
    private ExportReceiptDetailRepository exportReceiptDetailRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @InjectMocks
    private ExportReceiptServiceImpl exportReceiptService;

    private Employee manager;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        Role managerRole = new Role();
        managerRole.setCode(RoleCode.MANAGER);

        manager = new Employee();
        manager.setId(7L);
        manager.setFullName("Tran Thi Quan Ly");
        manager.setStatus(EmployeeStatus.HOAT_DONG);
        manager.setRole(managerRole);

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Kho tong");

        authenticate(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null, List.of()));
    }

    private Employee employeeWith(RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee employee = new Employee();
        employee.setId(99L);
        employee.setFullName("Nhan vien");
        employee.setStatus(EmployeeStatus.HOAT_DONG);
        employee.setRole(role);
        return employee;
    }

    private ExportReceipt receiptWithStatus(ExportReceiptStatus status) {
        ExportReceipt receipt = new ExportReceipt();
        receipt.setId(100L);
        receipt.setCode("XUAT-001");
        receipt.setStatus(status);
        receipt.setWarehouse(warehouse);
        return receipt;
    }

    @Test
    void reject_pendingReceiptShouldMoveToRejectedAndStoreMetadata() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());

        ExportReceiptDetailResponse response = exportReceiptService.reject(100L,
                new RejectExportReceiptRequest("Sai thông tin khách hàng"));

        assertEquals("TU_CHOI", response.status());
        assertEquals(ExportReceiptStatus.TU_CHOI, receipt.getStatus());
        assertEquals("Sai thông tin khách hàng", receipt.getRejectionReason());
        assertEquals(manager, receipt.getRejectedBy());
        assertNotNull(receipt.getRejectedAt());
    }

    @Test
    void reject_withoutReasonShouldThrowBadRequest() {
        assertThrows(BadRequestException.class,
                () -> exportReceiptService.reject(100L, new RejectExportReceiptRequest("   ")));
        verify(exportReceiptRepository, never()).findById(any());
    }

    @Test
    void reject_withWrongStatusShouldThrowConflict() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.HOAN_THANH);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class,
                () -> exportReceiptService.reject(100L, new RejectExportReceiptRequest("Lý do")));
        verify(exportReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void approve_level1ShouldMoveToPendingLevel2WithoutRecordingApproverMetadata() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());

        ExportReceiptDetailResponse response = exportReceiptService.approve(100L);

        assertEquals("CHO_DUYET_CAP_2", response.status());
        assertEquals(ExportReceiptStatus.CHO_DUYET_CAP_2, receipt.getStatus());
        assertNull(receipt.getApprovedBy());
        assertNull(receipt.getApprovedAt());
        verify(inventoryLevelRepository, never()).findByProductIdAndWarehouseId(any(), any());
    }

    @Test
    void approve_withDetailsShouldUseBatchInventoryLookup() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        Product product = new Product();
        product.setId(10L);
        ExportReceiptDetail detail = new ExportReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(5);
        receipt.setWarehouse(warehouse);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByWarehouseIdAndProductIdIn(eq(1L), anyList()))
                .thenReturn(List.of());

        exportReceiptService.approve(100L);

        verify(inventoryLevelRepository).findByWarehouseIdAndProductIdIn(eq(1L), anyList());
        verify(inventoryLevelRepository, never()).findByProductIdAndWarehouseId(any(), any());
    }

    @Test
    void approve_pendingLevel2ShouldCompleteAndDeductInventory() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
        Product product = new Product();
        product.setId(10L);
        ExportReceiptDetail detail = new ExportReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(3);
        InventoryLevel inventoryLevel = new InventoryLevel();
        inventoryLevel.setProduct(product);
        inventoryLevel.setWarehouse(warehouse);
        inventoryLevel.setQuantity(5);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(inventoryLevel));
        when(inventoryTransactionService.recordTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new InventoryTransaction());

        ExportReceiptDetailResponse response = exportReceiptService.approve(100L);

        assertEquals("HOAN_THANH", response.status());
        assertEquals(ExportReceiptStatus.HOAN_THANH, receipt.getStatus());
        assertEquals(manager, receipt.getApprovedBy());
        assertEquals(2, inventoryLevel.getQuantity());
        verify(inventoryTransactionService).recordTransaction(eq(10L), eq(1L), eq(InventoryTransactionType.XUAT_KHO),
                eq(3), eq(5), eq(2), any(), any());
    }

    @Test
    void approve_whenInventoryInsufficientShouldThrowConflict() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
        Product product = new Product();
        product.setId(10L);
        ExportReceiptDetail detail = new ExportReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(3);
        InventoryLevel inventoryLevel = new InventoryLevel();
        inventoryLevel.setProduct(product);
        inventoryLevel.setWarehouse(warehouse);
        inventoryLevel.setQuantity(2);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(inventoryLevel));

        assertThrows(ConflictException.class, () -> exportReceiptService.approve(100L));
        verify(inventoryTransactionService, never()).recordTransaction(any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void approve_withWrongStatusShouldThrowConflict() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.HOAN_THANH);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> exportReceiptService.approve(100L));
        verify(exportReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void approve_withMissingReceiptShouldThrowNotFound() {
        when(exportReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> exportReceiptService.approve(404L));
    }

    @Test
    void approve_withEmployeeRoleShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));

        assertThrows(MissingRoleException.class, () -> exportReceiptService.approve(100L));
        verify(exportReceiptRepository, never()).findById(any());
    }

    @Test
    void approve_withInactiveAccountShouldThrowAccountInactive() {
        manager.setStatus(EmployeeStatus.TAM_KHOA);

        assertThrows(AccountInactiveException.class, () -> exportReceiptService.approve(100L));
        verify(exportReceiptRepository, never()).findById(any());
    }
}
