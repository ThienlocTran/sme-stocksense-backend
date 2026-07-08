package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ExportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void approve_level1ShouldMoveToPendingLevel2AndRecordApprover() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
        when(exportReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(100L)).thenReturn(List.of());

        ExportReceiptDetailResponse response = exportReceiptService.approve(100L);

        assertEquals("CHO_DUYET_CAP_2", response.status());
        assertEquals(ExportReceiptStatus.CHO_DUYET_CAP_2, receipt.getStatus());
        assertEquals(manager, receipt.getApprovedBy());
        assertNotNull(receipt.getApprovedAt());
        verify(inventoryLevelRepository, never()).findByProductIdAndWarehouseId(any(), any());
    }

    @Test
    void approve_withWrongStatusShouldThrowConflict() {
        ExportReceipt receipt = receiptWithStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
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
