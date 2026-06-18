package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptSubmitServiceTest {

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
    private Warehouse warehouse;
    private Partner supplier;
    private Product product;
    private ImportReceipt receipt;
    private ImportReceiptDetail detail;

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
                amountCalculator
        );
        owner = employee(5L, RoleCode.EMPLOYEE);
        warehouse = warehouse(1L, WarehouseStatus.HOAT_DONG);
        supplier = supplier(10L, PartnerStatus.HOAT_DONG, PartnerType.NHA_CUNG_CAP);
        product = product(25L, ProductStatus.HOAT_DONG);
        receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        detail = detail(1001L, receipt, product, 10, new BigDecimal("125000.00"));
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitForApproval_ownerWithValidDraftShouldMoveToWaitingLevel1() {
        stubSuccessfulSubmit(owner);

        ImportReceiptDraftResponse response = importReceiptService.submitForApproval(123L);

        assertEquals("CHO_DUYET_CAP_1", response.status());
        assertEquals(ImportReceiptStatus.CHO_DUYET_CAP_1, receipt.getStatus());
        assertEquals(owner, receipt.getSubmittedBy());
        assertNotNull(receipt.getSubmittedAt());
        assertEquals(owner.getId(), response.submittedById());
        assertNotNull(response.submittedAt());
        assertEquals(new BigDecimal("1250000.00"), receipt.getTotalAmount());
        assertEquals(new BigDecimal("1250000.00"), detail.getExpectedLineTotal());
    }

    @Test
    void submitForApproval_adminWithValidDraftShouldSucceed() {
        Employee admin = employee(1L, RoleCode.ADMIN);
        authenticate(admin);
        stubSuccessfulSubmit(admin);

        importReceiptService.submitForApproval(123L);

        assertEquals(admin, receipt.getSubmittedBy());
        assertEquals(ImportReceiptStatus.CHO_DUYET_CAP_1, receipt.getStatus());
    }

    @Test
    void submitForApproval_rejectedReceiptShouldThrowConflictBecauseWorkflowRequiresEditBackToDraft() {
        assertStatusConflict(ImportReceiptStatus.TU_CHOI);
    }

    @Test
    void submitForApproval_employeeCannotSubmitOtherEmployeeReceipt() {
        authenticate(employee(6L, RoleCode.EMPLOYEE));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> importReceiptService.submitForApproval(123L));
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void submitForApproval_managerCannotSubmitEmployeeReceipt() {
        authenticate(employee(7L, RoleCode.MANAGER));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> importReceiptService.submitForApproval(123L));
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void submitForApproval_missingReceiptShouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.submitForApproval(404L));
    }

    @Test
    void submitForApproval_nonDraftStatusesShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.CHO_DUYET_CAP_1);
        assertStatusConflict(ImportReceiptStatus.CHO_DUYET_CAP_2);
        assertStatusConflict(ImportReceiptStatus.CHO_HANG_VE);
        assertStatusConflict(ImportReceiptStatus.CHO_KIEM_HANG);
        assertStatusConflict(ImportReceiptStatus.HOAN_THANH);
        assertStatusConflict(ImportReceiptStatus.HUY);
    }

    @Test
    void submitForApproval_withoutDetailsShouldThrowConflictAndNotMutate() {
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of());

        assertThrows(ConflictException.class, () -> importReceiptService.submitForApproval(123L));

        assertEquals(ImportReceiptStatus.NHAP, receipt.getStatus());
        assertNull(receipt.getSubmittedBy());
        assertNull(receipt.getSubmittedAt());
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void submitForApproval_withInactiveProductShouldThrowBadRequest() {
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product(25L, ProductStatus.NGUNG_HOAT_DONG)));

        assertThrows(BadRequestException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_withInvalidQuantityShouldThrowBadRequest() {
        detail.setExpectedQuantity(0);
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_withInvalidUnitPriceShouldThrowBadRequest() {
        detail.setExpectedUnitPrice(new BigDecimal("-1.00"));
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_withDuplicateProductShouldThrowConflict() {
        ImportReceiptDetail duplicate = detail(1002L, receipt, product, 1, BigDecimal.ONE);
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail, duplicate));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));

        assertThrows(ConflictException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_withInactiveWarehouseShouldThrowBadRequest() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, WarehouseStatus.NGUNG_HOAT_DONG)));

        assertThrows(BadRequestException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_withInactiveSupplierShouldThrowBadRequest() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, PartnerStatus.NGUNG_HOAT_DONG, PartnerType.NHA_CUNG_CAP)));

        assertThrows(BadRequestException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_withWrongSupplierTypeShouldThrowBadRequest() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, PartnerStatus.HOAT_DONG, PartnerType.KHACH_HANG)));

        assertThrows(BadRequestException.class, () -> importReceiptService.submitForApproval(123L));
    }

    @Test
    void submitForApproval_shouldKeepHeaderAndNotWriteApprovalFields() {
        stubSuccessfulSubmit(owner);

        importReceiptService.submitForApproval(123L);

        assertEquals("PNK-001", receipt.getCode());
        assertEquals(owner, receipt.getCreatedBy());
        assertEquals(warehouse, receipt.getWarehouse());
        assertEquals(supplier, receipt.getSupplier());
        assertEquals("Can duyet", receipt.getNote());
        assertNull(receipt.getLevel1ApprovedBy());
        assertNull(receipt.getLevel1ApprovedAt());
        assertNull(receipt.getLevel2ApprovedBy());
        assertNull(receipt.getLevel2ApprovedAt());
    }

    @Test
    void submitForApproval_whenVersionConflictsShouldThrowConflict() {
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptRepository.saveAndFlush(receipt)).thenThrow(new OptimisticLockingFailureException("version"));

        assertThrows(ConflictException.class, () -> importReceiptService.submitForApproval(123L));
    }

    private void stubSuccessfulSubmit(Employee actor) {
        stubHeaderAndMasterData();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptRepository.saveAndFlush(receipt)).thenAnswer(invocation -> {
            receipt.setSubmittedBy(actor);
            return receipt;
        });
    }

    private void stubHeaderAndMasterData() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));
    }

    private void assertStatusConflict(ImportReceiptStatus status) {
        receipt.setStatus(status);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> importReceiptService.submitForApproval(123L));
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
        verify(importReceiptDetailRepository, never()).saveAllAndFlush(anyList());

        receipt.setStatus(ImportReceiptStatus.NHAP);
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

    private ImportReceipt receipt(Long id, Employee creator, ImportReceiptStatus status) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(id);
        receipt.setCode("PNK-001");
        receipt.setCreatedBy(creator);
        receipt.setWarehouse(warehouse);
        receipt.setSupplier(supplier);
        receipt.setStatus(status);
        receipt.setNote("Can duyet");
        receipt.setRejectionReason("Ly do cu");
        receipt.setTotalAmount(BigDecimal.ZERO);
        receipt.setVersion(1L);
        return receipt;
    }

    private ImportReceiptDetail detail(Long id, ImportReceipt receipt, Product product, Integer quantity, BigDecimal unitPrice) {
        ImportReceiptDetail detail = new ImportReceiptDetail();
        detail.setId(id);
        detail.setDocument(receipt);
        detail.setProduct(product);
        detail.setExpectedQuantity(quantity);
        detail.setExpectedUnitPrice(unitPrice);
        detail.setExpectedLineTotal(BigDecimal.ZERO);
        detail.setNote("Lo 1");
        return detail;
    }

    private Warehouse warehouse(Long id, WarehouseStatus status) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("Kho tong");
        warehouse.setStatus(status);
        return warehouse;
    }

    private Partner supplier(Long id, PartnerStatus status, PartnerType type) {
        Partner supplier = new Partner();
        supplier.setId(id);
        supplier.setName("Nha cung cap A");
        supplier.setStatus(status);
        supplier.setType(type);
        return supplier;
    }

    private Product product(Long id, ProductStatus status) {
        Product product = new Product();
        product.setId(id);
        product.setCode("SP-001");
        product.setName("Ca phe rang xay");
        product.setStatus(status);
        return product;
    }
}
