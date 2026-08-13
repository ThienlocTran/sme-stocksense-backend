package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.CancelReceiptRequest;
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
import org.springframework.security.core.context.SecurityContext;
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
import static org.mockito.ArgumentMatchers.same;
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
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(employee, null, List.of()));
        SecurityContextHolder.setContext(context);
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
        Employee creator = employeeWith(RoleCode.EMPLOYEE);
        creator.setId(5L);
        receipt.setCreatedBy(creator);
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
    void reject_withEmployeeRoleShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));

        assertThrows(MissingRoleException.class,
                () -> exportReceiptService.reject(100L, new RejectExportReceiptRequest("Lý do")));
        verify(exportReceiptRepository, never()).findById(any());
    }

    @Test
    void reject_withInactiveAccountShouldThrowAccountInactive() {
        manager.setStatus(EmployeeStatus.TAM_KHOA);

        assertThrows(AccountInactiveException.class,
                () -> exportReceiptService.reject(100L, new RejectExportReceiptRequest("Lý do")));
        verify(exportReceiptRepository, never()).findById(any());
    }

    @Test
    void reject_withMissingReceiptShouldThrowNotFound() {
        when(exportReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> exportReceiptService.reject(404L, new RejectExportReceiptRequest("Lý do")));
    }

    @Test
    void approve_legacyLevel1ShouldCompleteAsLevel2Approval() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());

        ExportReceiptDetailResponse response = exportReceiptService.approve(100L);

        assertEquals("HOAN_THANH", response.status());
        assertEquals(ExportReceiptStatus.HOAN_THANH, receipt.getStatus());
        assertEquals(manager, receipt.getApprovedBy());
        assertNotNull(receipt.getApprovedAt());
        verify(inventoryLevelRepository, never()).findByProductIdAndWarehouseId(any(), any());
    }

    @Test
    void approve_legacyLevel1WithDetailsShouldLockAndDeductInventory() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        Product product = new Product();
        product.setId(10L);
        ExportReceiptDetail detail = new ExportReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(5);
        InventoryLevel inventoryLevel = new InventoryLevel();
        inventoryLevel.setProduct(product);
        inventoryLevel.setWarehouse(warehouse);
        inventoryLevel.setQuantity(8);
        receipt.setWarehouse(warehouse);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(inventoryLevel));

        exportReceiptService.approve(100L);

        verify(inventoryLevelRepository).findByProductIdAndWarehouseIdForUpdate(10L, 1L);
        assertEquals(3, inventoryLevel.getQuantity());
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
        when(inventoryTransactionService.recordExportTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new InventoryTransaction());

        ExportReceiptDetailResponse response = exportReceiptService.approve(100L);

        assertEquals("HOAN_THANH", response.status());
        assertEquals(ExportReceiptStatus.HOAN_THANH, receipt.getStatus());
        assertEquals(manager, receipt.getApprovedBy());
        assertEquals(2, inventoryLevel.getQuantity());
        verify(inventoryTransactionService).recordExportTransaction(eq(10L), eq(1L), eq(InventoryTransactionType.XUAT_KHO),
                eq(3), eq(5), eq(2), same(receipt), eq("Duyet phieu xuat cap 2"));
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
        verify(inventoryTransactionService, never()).recordExportTransaction(any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    @Test
    void approve_whenTransactionRecordingFailsShouldNotCompleteReceipt() {
        manager.setStatus(EmployeeStatus.HOAT_DONG);
        authenticate(manager);
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
        Product product = new Product();
        product.setId(10L);
        ExportReceiptDetail detail = new ExportReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(2);
        InventoryLevel inventoryLevel = new InventoryLevel();
        inventoryLevel.setProduct(product);
        inventoryLevel.setWarehouse(warehouse);
        inventoryLevel.setQuantity(5);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(inventoryLevel));
        when(inventoryTransactionService.recordExportTransaction(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Khong ghi duoc giao dich"));

        assertThrows(IllegalStateException.class, () -> {
            manager.setStatus(EmployeeStatus.HOAT_DONG);
            authenticate(manager);
            exportReceiptService.approve(100L);
        });

        assertEquals(ExportReceiptStatus.CHO_DUYET_CAP_2, receipt.getStatus());
        verify(exportReceiptRepository, never()).saveAndFlush(any());
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
    void approve_creatorShouldNotSelfApprove() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        receipt.setCreatedBy(manager);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(BadRequestException.class, () -> exportReceiptService.approve(100L));
        verify(exportReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void approve_withInactiveAccountShouldThrowAccountInactive() {
        manager.setStatus(EmployeeStatus.TAM_KHOA);

        assertThrows(AccountInactiveException.class, () -> exportReceiptService.approve(100L));
        verify(exportReceiptRepository, never()).findById(any());
    }

    @Test
    void cancelMidState_adminWithApprovedReceiptShouldCancelWithoutInventoryDeduction() {
        Employee admin = employeeWith(RoleCode.ADMIN);
        admin.setId(1L);
        authenticate(admin);
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_XUAT);
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
        when(exportReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByWarehouseIdAndProductIdIn(1L, List.of(10L))).thenReturn(List.of(inventoryLevel));

        ExportReceiptDetailResponse response = exportReceiptService.cancel(100L, new CancelReceiptRequest("Khach huy don"));

        assertEquals("HUY", response.status());
        assertEquals(5, inventoryLevel.getQuantity());
        assertEquals(admin, receipt.getCancelledBy());
        assertNotNull(receipt.getCancelledAt());
        verify(inventoryLevelRepository, never()).findByProductIdAndWarehouseIdForUpdate(any(), any());
        verify(inventoryTransactionService, never()).recordExportTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelMidState_managerWithApprovedReceiptShouldCancel() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_XUAT);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());

        ExportReceiptDetailResponse response = exportReceiptService.cancel(100L, new CancelReceiptRequest("Khach huy"));

        assertEquals("HUY", response.status());
        assertEquals(manager, receipt.getCancelledBy());
    }

    @Test
    void cancelMidState_employeeShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_XUAT);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> exportReceiptService.cancel(100L, new CancelReceiptRequest("Ly do")));
        verify(exportReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelMidState_withoutReasonShouldThrowBadRequest() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_XUAT);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(BadRequestException.class, () -> exportReceiptService.cancel(100L, new CancelReceiptRequest(" ")));
        verify(exportReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelMidState_completedReceiptShouldThrowConflict() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.HOAN_THANH);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> exportReceiptService.cancel(100L, new CancelReceiptRequest("Ly do")));
        verify(exportReceiptRepository, never()).saveAndFlush(any());
    }
}
