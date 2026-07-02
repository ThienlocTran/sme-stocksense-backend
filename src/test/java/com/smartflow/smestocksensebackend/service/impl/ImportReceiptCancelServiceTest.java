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
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptCancelServiceTest {

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
    private ImportReceipt receipt;
    private List<ImportReceiptDetail> details;

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
                null
        );
        owner = employee(5L, RoleCode.EMPLOYEE);
        warehouse = warehouse(1L);
        supplier = supplier(10L);
        receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        details = List.of(detail(1001L, receipt));
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancelDraft_ownerWithDraftReceiptShouldSetCancelledStatus() {
        stubSuccessfulCancel();

        ImportReceiptDraftResponse response = importReceiptService.cancelDraft(123L);

        assertEquals("HUY", response.status());
        assertEquals(ImportReceiptStatus.HUY, receipt.getStatus());
        assertEquals(owner, receipt.getCancelledBy());
        assertNotNull(receipt.getCancelledAt());
    }

    @Test
    void cancelDraft_adminWithDraftReceiptShouldSucceed() {
        Employee admin = employee(1L, RoleCode.ADMIN);
        authenticate(admin);
        stubSuccessfulCancel();

        ImportReceiptDraftResponse response = importReceiptService.cancelDraft(123L);

        assertEquals("HUY", response.status());
        assertEquals(admin, receipt.getCancelledBy());
    }

    @Test
    void cancelDraft_employeeCannotCancelOtherEmployeeReceipt() {
        authenticate(employee(6L, RoleCode.EMPLOYEE));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> importReceiptService.cancelDraft(123L));
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void cancelDraft_managerCannotCancelEmployeeReceipt() {
        authenticate(employee(7L, RoleCode.MANAGER));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> importReceiptService.cancelDraft(123L));
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void cancelDraft_missingReceiptShouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.cancelDraft(404L));
    }

    @Test
    void cancelDraft_rejectedReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.TU_CHOI);
    }

    @Test
    void cancelDraft_waitingLevel1ReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.CHO_DUYET_CAP_1);
    }

    @Test
    void cancelDraft_waitingLevel2ReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.CHO_DUYET_CAP_2);
    }

    @Test
    void cancelDraft_waitingGoodsReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.CHO_HANG_VE);
    }

    @Test
    void cancelDraft_cancelledReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.HUY);
    }

    @Test
    void cancelDraft_completedReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.HOAN_THANH);
    }

    @Test
    void cancelDraft_checkingGoodsReceiptShouldThrowConflict() {
        assertStatusConflict(ImportReceiptStatus.CHO_KIEM_HANG);
    }

    @Test
    void cancelDraft_shouldKeepCodeCreatorWarehouseSupplierNoteTotalAndDetails() {
        stubSuccessfulCancel();

        ImportReceiptDraftResponse response = importReceiptService.cancelDraft(123L);

        assertEquals("PNK-001", receipt.getCode());
        assertEquals(owner, receipt.getCreatedBy());
        assertEquals(warehouse, receipt.getWarehouse());
        assertEquals(supplier, receipt.getSupplier());
        assertEquals("Ghi chu cu", receipt.getNote());
        assertEquals(new BigDecimal("1250000.00"), receipt.getTotalAmount());
        assertEquals(1, response.detailCount());
        assertEquals(1001L, response.details().getFirst().id());
        assertEquals(new BigDecimal("1250000.00"), response.totalAmount());
    }

    @Test
    void cancelDraft_shouldNotWriteSubmitOrApprovalFields() {
        stubSuccessfulCancel();

        importReceiptService.cancelDraft(123L);

        assertEquals(null, receipt.getSubmittedBy());
        assertEquals(null, receipt.getSubmittedAt());
        assertEquals(null, receipt.getLevel1ApprovedBy());
        assertEquals(null, receipt.getLevel1ApprovedAt());
        assertEquals(null, receipt.getLevel2ApprovedBy());
        assertEquals(null, receipt.getLevel2ApprovedAt());
    }

    @Test
    void cancelDraft_shouldNotMutateDetailOrTotalCalculations() {
        stubSuccessfulCancel();

        importReceiptService.cancelDraft(123L);

        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
        verify(importReceiptDetailRepository, never()).saveAllAndFlush(anyList());
        verify(importReceiptDetailRepository, never()).sumLineTotalByReceiptId(123L);
    }

    @Test
    void cancelDraft_shouldNotUseInventoryOrStockTransactionRepositories() throws NoSuchFieldException {
        ImportReceiptServiceImpl.class.getDeclaredField("importReceiptRepository");
        ImportReceiptServiceImpl.class.getDeclaredField("importReceiptDetailRepository");
        assertThrows(NoSuchFieldException.class, () -> ImportReceiptServiceImpl.class.getDeclaredField("inventoryRepository"));
        assertThrows(NoSuchFieldException.class, () -> ImportReceiptServiceImpl.class.getDeclaredField("stockTransactionRepository"));
    }

    @Test
    void cancelDraft_whenVersionConflictsShouldThrowConflict() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(receipt)).thenThrow(new OptimisticLockingFailureException("version"));

        assertThrows(ConflictException.class, () -> importReceiptService.cancelDraft(123L));
    }

    private void stubSuccessfulCancel() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(details);
    }

    private void assertStatusConflict(ImportReceiptStatus status) {
        receipt.setStatus(status);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> importReceiptService.cancelDraft(123L));
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
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
        receipt.setNote("Ghi chu cu");
        receipt.setTotalAmount(new BigDecimal("1250000.00"));
        receipt.setVersion(1L);
        return receipt;
    }

    private ImportReceiptDetail detail(Long id, ImportReceipt receipt) {
        Product product = new Product();
        product.setId(25L);
        product.setCode("SP-001");
        product.setName("Ca phe rang xay");

        ImportReceiptDetail detail = new ImportReceiptDetail();
        detail.setId(id);
        detail.setDocument(receipt);
        detail.setProduct(product);
        detail.setExpectedQuantity(10);
        detail.setExpectedUnitPrice(new BigDecimal("125000.00"));
        detail.setExpectedLineTotal(new BigDecimal("1250000.00"));
        detail.setNote("Lo 1");
        return detail;
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("Kho tong");
        return warehouse;
    }

    private Partner supplier(Long id) {
        Partner supplier = new Partner();
        supplier.setId(id);
        supplier.setName("Nha cung cap A");
        return supplier;
    }
}
