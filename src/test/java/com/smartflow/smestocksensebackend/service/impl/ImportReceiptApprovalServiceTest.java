package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.RejectImportReceiptRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho luồng duyệt phiếu nhập theo cấp (T91..T94):
 * listPendingApproval, getApprovalDetail, approve, reject.
 */
@ExtendWith(MockitoExtension.class)
class ImportReceiptApprovalServiceTest {

    @Mock
    private ImportReceiptRepository importReceiptRepository;

    @Mock
    private ImportReceiptDetailRepository importReceiptDetailRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ImportReceiptServiceImpl importReceiptService;

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

        ReflectionTestUtils.setField(importReceiptService, "secondApprovalThreshold", BigDecimal.valueOf(50000000));
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

    private ImportReceipt receiptWithStatus(ImportReceiptStatus status) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(100L);
        receipt.setCode("PNK-001");
        receipt.setStatus(status);
        receipt.setWarehouse(warehouse);
        receipt.setVersion(1L);
        receipt.setTotalAmount(BigDecimal.valueOf(1000));
        Employee creator = employeeWith(RoleCode.EMPLOYEE);
        creator.setId(5L);
        receipt.setCreatedBy(creator);
        return receipt;
    }

    // ---------------------------------------------------------------------
    // T93 - approve
    // ---------------------------------------------------------------------

    @Test
    void approve_level1ShouldMoveToWaitingGoodsAndRecordManagerApprover() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        ImportReceiptDraftResponse response = importReceiptService.approve(100L);

        assertEquals(ImportReceiptStatus.CHO_HANG_VE.name(), response.status());
        assertEquals(ImportReceiptStatus.CHO_HANG_VE, receipt.getStatus());
        assertEquals(manager, receipt.getLevel1ApprovedBy());
        assertNotNull(receipt.getLevel1ApprovedAt());
        assertNull(receipt.getLevel2ApprovedBy());
        assertNull(receipt.getLevel2ApprovedAt());
        // Bước duyệt KHÔNG được cộng tồn kho.
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void approve_oldLevel2ShouldMoveToWaitingGoodsAndRecordManagerApprover() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_2);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        ImportReceiptDraftResponse response = importReceiptService.approve(100L);

        assertEquals(ImportReceiptStatus.CHO_HANG_VE.name(), response.status());
        assertEquals(ImportReceiptStatus.CHO_HANG_VE, receipt.getStatus());
        assertEquals(manager, receipt.getLevel2ApprovedBy());
        assertNotNull(receipt.getLevel2ApprovedAt());
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void approve_adminShouldBeAllowed() {
        authenticate(employeeWith(RoleCode.ADMIN));
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        ImportReceiptDraftResponse response = importReceiptService.approve(100L);

        assertEquals(ImportReceiptStatus.CHO_HANG_VE.name(), response.status());
    }

    @Test
    void approve_withWrongStatusShouldThrowConflict() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.NHAP);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> importReceiptService.approve(100L));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void approve_withMissingReceiptShouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.approve(404L));
    }

    @Test
    void approve_withEmployeeRoleShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));

        assertThrows(MissingRoleException.class, () -> importReceiptService.approve(100L));
        verify(importReceiptRepository, never()).findById(anyLong());
    }

    @Test
    void approve_withInactiveAccountShouldThrowAccountInactive() {
        manager.setStatus(EmployeeStatus.TAM_KHOA);

        assertThrows(AccountInactiveException.class, () -> importReceiptService.approve(100L));
        verify(importReceiptRepository, never()).findById(anyLong());
    }

    @Test
    void approve_withConcurrentUpdateShouldThrowConflict() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenThrow(new OptimisticLockingFailureException("version mismatch"));

        assertThrows(ConflictException.class, () -> importReceiptService.approve(100L));
    }

    // ---------------------------------------------------------------------
    // T94 - reject
    // ---------------------------------------------------------------------

    @Test
    void reject_fromLevel1ShouldMoveToRejectedWithReason() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        ImportReceiptDraftResponse response = importReceiptService.reject(100L,
                new RejectImportReceiptRequest("  Sai don gia nhap.  "));

        assertEquals(ImportReceiptStatus.TU_CHOI.name(), response.status());
        assertEquals(ImportReceiptStatus.TU_CHOI, receipt.getStatus());
        assertEquals("Sai don gia nhap.", receipt.getRejectionReason());
        assertEquals("Sai don gia nhap.", response.rejectionReason());
    }

    @Test
    void reject_fromLevel2ShouldMoveToRejected() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_2);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        ImportReceiptDraftResponse response = importReceiptService.reject(100L,
                new RejectImportReceiptRequest("Khong dat yeu cau."));

        assertEquals(ImportReceiptStatus.TU_CHOI.name(), response.status());
        assertEquals("Khong dat yeu cau.", receipt.getRejectionReason());
    }

    @Test
    void reject_withBlankReasonShouldThrowBadRequest() {
        assertThrows(BadRequestException.class,
                () -> importReceiptService.reject(100L, new RejectImportReceiptRequest("   ")));
        verify(importReceiptRepository, never()).findById(anyLong());
    }

    @Test
    void reject_withNullRequestShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> importReceiptService.reject(100L, null));
        verify(importReceiptRepository, never()).findById(anyLong());
    }

    @Test
    void reject_withWrongStatusShouldThrowConflict() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_HANG_VE);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class,
                () -> importReceiptService.reject(100L, new RejectImportReceiptRequest("Ly do.")));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void reject_withMissingReceiptShouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> importReceiptService.reject(404L, new RejectImportReceiptRequest("Ly do.")));
    }

    @Test
    void reject_withEmployeeRoleShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));

        assertThrows(MissingRoleException.class,
                () -> importReceiptService.reject(100L, new RejectImportReceiptRequest("Ly do.")));
        verify(importReceiptRepository, never()).findById(anyLong());
    }

    // ---------------------------------------------------------------------
    // T91 - listPendingApproval
    // ---------------------------------------------------------------------

    @Test
    void listPendingApproval_withoutStatusShouldQueryBothPendingStatuses() {
        Pageable pageable = PageRequest.of(0, 10);
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findByStatusIn(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));

        ImportReceiptPageResponse response = importReceiptService.listPendingApproval(null, pageable);

        assertEquals(1, response.totalElements());
        assertEquals("CHO_DUYET_CAP_1", response.content().get(0).status());

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<ImportReceiptStatus>> captor =
                org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(importReceiptRepository).findByStatusIn(captor.capture(), eq(pageable));
        assertEquals(2, captor.getValue().size());
        org.junit.jupiter.api.Assertions.assertTrue(
                captor.getValue().containsAll(List.of(
                        ImportReceiptStatus.CHO_DUYET_CAP_1, ImportReceiptStatus.CHO_DUYET_CAP_2)));
    }

    @Test
    void listPendingApproval_withValidStatusShouldQueryThatStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_2);
        when(importReceiptRepository.findByStatus(eq(ImportReceiptStatus.CHO_DUYET_CAP_2), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));

        ImportReceiptPageResponse response =
                importReceiptService.listPendingApproval("CHO_DUYET_CAP_2", pageable);

        assertEquals(1, response.totalElements());
        assertEquals("CHO_DUYET_CAP_2", response.content().get(0).status());
        verify(importReceiptRepository, never()).findByStatusIn(any(), any());
    }

    @Test
    void listPendingApproval_withNonApprovalStatusShouldThrowBadRequest() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(BadRequestException.class,
                () -> importReceiptService.listPendingApproval("NHAP", pageable));
        verify(importReceiptRepository, never()).findByStatus(any(), any());
        verify(importReceiptRepository, never()).findByStatusIn(any(), any());
    }

    @Test
    void listPendingApproval_withInvalidStatusStringShouldThrowBadRequest() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(BadRequestException.class,
                () -> importReceiptService.listPendingApproval("KHONG_TON_TAI", pageable));
    }

    @Test
    void listPendingApproval_withEmployeeRoleShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(MissingRoleException.class,
                () -> importReceiptService.listPendingApproval(null, pageable));
    }

    // ---------------------------------------------------------------------
    // T92 - getApprovalDetail
    // ---------------------------------------------------------------------

    @Test
    void getApprovalDetail_managerCanViewAnyReceipt() {
        ImportReceipt receipt = receiptWithStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        ImportReceiptDraftResponse response = importReceiptService.getApprovalDetail(100L);

        assertEquals(100L, response.id());
        assertEquals("CHO_DUYET_CAP_1", response.status());
    }

    @Test
    void getApprovalDetail_withMissingReceiptShouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.getApprovalDetail(404L));
    }

    @Test
    void getApprovalDetail_withEmployeeRoleShouldThrowMissingRole() {
        authenticate(employeeWith(RoleCode.EMPLOYEE));

        assertThrows(MissingRoleException.class, () -> importReceiptService.getApprovalDetail(100L));
        verify(importReceiptRepository, never()).findById(anyLong());
    }
}
