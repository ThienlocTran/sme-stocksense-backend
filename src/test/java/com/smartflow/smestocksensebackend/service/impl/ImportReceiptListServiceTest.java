package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptCodeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptListServiceTest {

    @Mock
    private ImportReceiptRepository importReceiptRepository;

    @Mock
    private ImportReceiptDetailRepository importReceiptDetailRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private ImportReceiptCodeGenerator codeGenerator;

    private ImportReceiptServiceImpl importReceiptService;
    private Employee owner;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        ImportReceiptItemValidator itemValidator = new ImportReceiptItemValidator(productRepository, importReceiptDetailRepository);
        ImportReceiptAmountCalculator amountCalculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);
        importReceiptService = new ImportReceiptServiceImpl(
                importReceiptRepository,
                importReceiptDetailRepository,
                warehouseRepository,
                partnerRepository,
                codeGenerator,
                itemValidator,
                amountCalculator,
                null,
                null,
                null,
                null,
                null,
                null
        );
        owner = employee(5L, RoleCode.EMPLOYEE);
        pageable = PageRequest.of(0, 10);
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMyReceipts_employeeShouldOnlyQueryOwnReceipts() {
        ImportReceipt draft = receipt(123L, owner, ImportReceiptStatus.NHAP, "PNK-001");
        ImportReceipt waiting = receipt(124L, owner, ImportReceiptStatus.CHO_DUYET_CAP_1, "PNK-002");
        when(importReceiptRepository.findByCreatedById(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(draft, waiting), pageable, 2));

        ImportReceiptPageResponse response = importReceiptService.listMyReceipts(null, pageable);

        assertEquals(2, response.totalElements());
        assertEquals("PNK-001", response.content().get(0).code());
        assertEquals("NHAP", response.content().get(0).status());
        assertEquals("CHO_DUYET_CAP_1", response.content().get(1).status());
        verify(importReceiptRepository).findByCreatedById(5L, pageable);
        verify(importReceiptRepository, never()).findAll();
    }

    @Test
    void listMyReceipts_shouldMapSummaryFields() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.TU_CHOI, "PNK-001");
        when(importReceiptRepository.findByCreatedById(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));

        ImportReceiptPageResponse response = importReceiptService.listMyReceipts(null, pageable);

        assertEquals(123L, response.content().getFirst().id());
        assertEquals("PNK-001", response.content().getFirst().code());
        assertEquals(1L, response.content().getFirst().warehouseId());
        assertEquals("Kho tong", response.content().getFirst().warehouseName());
        assertEquals(10L, response.content().getFirst().supplierId());
        assertEquals("Nha cung cap A", response.content().getFirst().supplierName());
        assertEquals(5L, response.content().getFirst().createdById());
        assertEquals("Nguyen Van A", response.content().getFirst().createdByName());
        assertEquals(new BigDecimal("1250000.00"), response.content().getFirst().totalAmount());
        assertEquals("Can xu ly", response.content().getFirst().note());
        assertEquals("Sai don gia nhap.", response.content().getFirst().rejectionReason());
        assertEquals(LocalDateTime.of(2026, 6, 18, 9, 0), response.content().getFirst().createdAt());
        assertEquals(LocalDateTime.of(2026, 6, 18, 10, 0), response.content().getFirst().updatedAt());
        assertEquals(LocalDateTime.of(2026, 6, 18, 11, 0), response.content().getFirst().submittedAt());
        assertEquals(LocalDateTime.of(2026, 6, 18, 12, 0), response.content().getFirst().cancelledAt());
    }

    @Test
    void listMyReceipts_adminShouldQueryOnlyAdminActorReceipts() {
        Employee admin = employee(1L, RoleCode.ADMIN);
        authenticate(admin);
        when(importReceiptRepository.findByCreatedById(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(receipt(200L, admin, ImportReceiptStatus.HUY, "PNK-ADMIN")), pageable, 1));

        ImportReceiptPageResponse response = importReceiptService.listMyReceipts(null, pageable);

        assertEquals(1, response.totalElements());
        assertEquals(1L, response.content().getFirst().createdById());
        assertEquals("PNK-ADMIN", response.content().getFirst().code());
        verify(importReceiptRepository).findByCreatedById(1L, pageable);
    }

    @Test
    void listMyReceipts_managerShouldThrowForbidden() {
        authenticate(employee(7L, RoleCode.MANAGER));

        assertThrows(MissingRoleException.class, () -> importReceiptService.listMyReceipts(null, pageable));

        verify(importReceiptRepository, never()).findByCreatedById(any(), any());
    }

    @Test
    void listReceipts_managerShouldQueryAllReceipts() {
        Employee manager = employee(7L, RoleCode.MANAGER);
        authenticate(manager);
        when(importReceiptRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(receipt(300L, owner, ImportReceiptStatus.HOAN_THANH, "PNK-ALL")), pageable, 1));

        ImportReceiptPageResponse response = importReceiptService.listReceipts(null, pageable);

        assertEquals(1, response.totalElements());
        assertEquals("PNK-ALL", response.content().getFirst().code());
        verify(importReceiptRepository).findAll(pageable);
    }

    @Test
    void listReceipts_adminShouldFilterByStatus() {
        Employee admin = employee(1L, RoleCode.ADMIN);
        authenticate(admin);
        when(importReceiptRepository.findByStatus(ImportReceiptStatus.NHAP, pageable))
                .thenReturn(new PageImpl<>(List.of(receipt(301L, owner, ImportReceiptStatus.NHAP, "PNK-NHAP")), pageable, 1));

        ImportReceiptPageResponse response = importReceiptService.listReceipts("NHAP", pageable);

        assertEquals(1, response.totalElements());
        assertEquals("NHAP", response.content().getFirst().status());
        verify(importReceiptRepository).findByStatus(ImportReceiptStatus.NHAP, pageable);
    }

    @Test
    void listReceipts_employeeShouldThrowForbidden() {
        assertThrows(MissingRoleException.class, () -> importReceiptService.listReceipts(null, pageable));

        verify(importReceiptRepository, never()).findAll(pageable);
    }

    @Test
    void listMyReceipts_shouldReturnMultipleStatuses() {
        when(importReceiptRepository.findByCreatedById(5L, pageable)).thenReturn(new PageImpl<>(List.of(
                receipt(1L, owner, ImportReceiptStatus.NHAP, "PNK-001"),
                receipt(2L, owner, ImportReceiptStatus.CHO_DUYET_CAP_1, "PNK-002"),
                receipt(3L, owner, ImportReceiptStatus.TU_CHOI, "PNK-003"),
                receipt(4L, owner, ImportReceiptStatus.HUY, "PNK-004")
        ), pageable, 4));

        ImportReceiptPageResponse response = importReceiptService.listMyReceipts(null, pageable);

        assertEquals(List.of("NHAP", "CHO_DUYET_CAP_1", "TU_CHOI", "HUY"), response.content().stream()
                .map(item -> item.status())
                .toList());
    }

    @Test
    void listMyReceipts_shouldReturnEmptyPageWhenEmployeeHasNoReceipt() {
        when(importReceiptRepository.findByCreatedById(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        ImportReceiptPageResponse response = importReceiptService.listMyReceipts(null, pageable);

        assertEquals(0, response.totalElements());
        assertEquals(List.of(), response.content());
    }

    @Test
    void listMyReceipts_shouldFilterByStatus() {
        when(importReceiptRepository.findByCreatedByIdAndStatus(5L, ImportReceiptStatus.NHAP, pageable))
                .thenReturn(new PageImpl<>(List.of(receipt(123L, owner, ImportReceiptStatus.NHAP, "PNK-001")), pageable, 1));

        ImportReceiptPageResponse response = importReceiptService.listMyReceipts("NHAP", pageable);

        assertEquals(1, response.totalElements());
        assertEquals("NHAP", response.content().getFirst().status());
        verify(importReceiptRepository).findByCreatedByIdAndStatus(5L, ImportReceiptStatus.NHAP, pageable);
        verify(importReceiptRepository, never()).findByCreatedById(5L, pageable);
    }

    @Test
    void listMyReceipts_withInvalidStatusShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> importReceiptService.listMyReceipts("INVALID", pageable));

        verify(importReceiptRepository, never()).findByCreatedById(any(), any());
        verify(importReceiptRepository, never()).findByCreatedByIdAndStatus(any(), any(), any());
    }

    @Test
    void listMyReceipts_shouldNotMutateData() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.NHAP, "PNK-001");
        when(importReceiptRepository.findByCreatedById(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));

        importReceiptService.listMyReceipts(null, pageable);

        assertEquals(ImportReceiptStatus.NHAP, receipt.getStatus());
        assertEquals(new BigDecimal("1250000.00"), receipt.getTotalAmount());
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    private void authenticate(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null, List.of())
        );
    }

    private Employee employee(Long id, RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName("Nguyen Van A");
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.HOAT_DONG);
        return employee;
    }

    private ImportReceipt receipt(Long id, Employee creator, ImportReceiptStatus status, String code) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(id);
        receipt.setCode(code);
        receipt.setWarehouse(warehouse());
        receipt.setSupplier(supplier());
        receipt.setCreatedBy(creator);
        receipt.setStatus(status);
        if (status == ImportReceiptStatus.TU_CHOI) {
            receipt.setRejectionReason("Sai don gia nhap.");
        }
        receipt.setTotalAmount(new BigDecimal("1250000.00"));
        receipt.setNote("Can xu ly");
        receipt.setSubmittedAt(LocalDateTime.of(2026, 6, 18, 11, 0));
        receipt.setCancelledAt(LocalDateTime.of(2026, 6, 18, 12, 0));
        receipt.setCreatedAt(LocalDateTime.of(2026, 6, 18, 9, 0));
        receipt.setUpdatedAt(LocalDateTime.of(2026, 6, 18, 10, 0));
        receipt.setVersion(1L);
        return receipt;
    }

    private Warehouse warehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Kho tong");
        return warehouse;
    }

    private Partner supplier() {
        Partner supplier = new Partner();
        supplier.setId(10L);
        supplier.setName("Nha cung cap A");
        return supplier;
    }
}
