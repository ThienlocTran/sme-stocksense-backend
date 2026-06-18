package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
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
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptDraftServiceTest {

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
    private ImportReceipt receipt;
    private Warehouse warehouse;
    private Partner supplier;
    private Product product;

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
        receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        warehouse = warehouse(1L, WarehouseStatus.HOAT_DONG);
        supplier = supplier(10L, PartnerStatus.HOAT_DONG, PartnerType.NHA_CUNG_CAP);
        product = product(25L, ProductStatus.HOAT_DONG);
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveDraft_ownerWithValidHeaderOnly_shouldSaveEmptyDraft() {
        stubValidHeader();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of());
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(BigDecimal.ZERO);
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        ImportReceiptDraftResponse response = importReceiptService.saveDraft(123L, new SaveImportReceiptDraftRequest(
                1L,
                10L,
                "  Phieu nhap du kien  ",
                null
        ));

        assertEquals(123L, response.id());
        assertEquals("PNK-001", response.code());
        assertEquals("NHAP", response.status());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertEquals(0, response.detailCount());
        assertEquals("Phieu nhap du kien", receipt.getNote());
        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
    }

    @Test
    void saveDraft_adminWithValidItems_shouldReplaceDetailsAndTotals() {
        authenticate(employee(1L, RoleCode.ADMIN));
        stubValidHeader();
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("1250000.00"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        ImportReceiptDraftResponse response = importReceiptService.saveDraft(123L, requestWithOneItem());

        assertEquals(1, response.detailCount());
        assertEquals(new BigDecimal("1250000.00"), receipt.getTotalAmount());
        assertEquals(new BigDecimal("1250000"), response.details().getFirst().lineTotal());
        verify(importReceiptDetailRepository).deleteByDocumentId(123L);

        ArgumentCaptor<List<ImportReceiptDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(importReceiptDetailRepository).saveAllAndFlush(captor.capture());
        ImportReceiptDetail saved = captor.getValue().getFirst();
        assertEquals(new BigDecimal("1250000"), saved.getExpectedLineTotal());
        assertEquals(receipt, saved.getDocument());
    }

    @Test
    void saveDraft_employeeWithOtherOwner_shouldThrowForbidden() {
        authenticate(employee(6L, RoleCode.EMPLOYEE));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));
    }

    @Test
    void saveDraft_withInactiveEmployee_shouldThrowBeforeMutatingDraft() {
        Employee inactiveEmployee = employee(5L, RoleCode.EMPLOYEE);
        inactiveEmployee.setStatus(EmployeeStatus.NGUNG_HOAT_DONG);
        authenticate(inactiveEmployee);

        assertThrows(AccountInactiveException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));

        assertNull(receipt.getWarehouse());
        assertNull(receipt.getSupplier());
        assertNull(receipt.getNote());
        verify(importReceiptRepository, never()).findById(123L);
        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
        verify(importReceiptDetailRepository, never()).saveAllAndFlush(anyList());
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void saveDraft_withMissingReceipt_shouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.saveDraft(404L, minimalRequest()));
    }

    @Test
    void saveDraft_withNonDraftReceipt_shouldThrowConflict() {
        receipt.setStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));
    }

    @Test
    void saveDraft_withInvalidWarehouse_shouldThrow() {
        stubReceiptOnly();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));

        warehouse.setStatus(WarehouseStatus.NGUNG_HOAT_DONG);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        assertThrows(BadRequestException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));
    }

    @Test
    void saveDraft_withInvalidSupplier_shouldThrow() {
        stubReceiptOnly();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));

        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, PartnerStatus.NGUNG_HOAT_DONG, PartnerType.NHA_CUNG_CAP)));
        assertThrows(BadRequestException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));

        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, PartnerStatus.HOAT_DONG, PartnerType.KHACH_HANG)));
        assertThrows(BadRequestException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));
    }

    @Test
    void saveDraft_withInvalidItem_shouldNotMutateDetails() {
        stubValidHeader();
        when(productRepository.findById(25L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.saveDraft(123L, requestWithOneItem()));

        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
        verify(importReceiptDetailRepository, never()).saveAllAndFlush(anyList());
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void saveDraft_withDuplicateProductInPayload_shouldThrowConflict() {
        stubValidHeader();
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));

        assertThrows(ConflictException.class, () -> importReceiptService.saveDraft(123L, new SaveImportReceiptDraftRequest(
                1L,
                10L,
                null,
                List.of(item(25L), item(25L))
        )));
    }

    @Test
    void saveDraft_shouldNotAcceptClientControlledFields() {
        List<String> fields = Arrays.stream(SaveImportReceiptDraftRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of("warehouseId", "supplierId", "note", "items"), fields);
        assertFalse(fields.contains("code"));
        assertFalse(fields.contains("status"));
        assertFalse(fields.contains("totalAmount"));
        assertFalse(fields.contains("version"));
    }

    @Test
    void saveDraft_shouldNotChangeCreatorCodeOrStatus() {
        stubValidHeader();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of());
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(BigDecimal.ZERO);
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        importReceiptService.saveDraft(123L, minimalRequest());

        assertEquals(owner, receipt.getCreatedBy());
        assertEquals("PNK-001", receipt.getCode());
        assertEquals(ImportReceiptStatus.NHAP, receipt.getStatus());
    }

    @Test
    void saveDraft_whenVersionConflicts_shouldReturnConflict() {
        stubValidHeader();
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of());
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(BigDecimal.ZERO);
        when(importReceiptRepository.saveAndFlush(receipt)).thenThrow(new OptimisticLockingFailureException("version") {
        });

        assertThrows(ConflictException.class, () -> importReceiptService.saveDraft(123L, minimalRequest()));
    }

    @Test
    void saveDraft_whenDetailSaveFails_shouldNotSaveHeader() {
        stubValidHeader();
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.saveAllAndFlush(anyList())).thenThrow(new DataIntegrityViolationException("detail failed"));

        assertThrows(DataIntegrityViolationException.class, () -> importReceiptService.saveDraft(123L, requestWithOneItem()));

        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void saveDraft_whenDetailDuplicateRace_shouldThrowConflict() {
        stubValidHeader();
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.saveAllAndFlush(anyList())).thenThrow(duplicateDetailException());

        assertThrows(ConflictException.class, () -> importReceiptService.saveDraft(123L, requestWithOneItem()));

        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void saveDraft_whenUnrelatedDataIntegrityViolationOccurs_shouldPropagate() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("foreign key failed");
        stubValidHeader();
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.saveAllAndFlush(anyList())).thenThrow(exception);

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> importReceiptService.saveDraft(123L, requestWithOneItem()));

        assertEquals(exception, thrown);
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    private void stubValidHeader() {
        stubReceiptOnly();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));
    }

    private void stubReceiptOnly() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
    }

    private SaveImportReceiptDraftRequest minimalRequest() {
        return new SaveImportReceiptDraftRequest(1L, 10L, "Phieu nhap du kien", null);
    }

    private SaveImportReceiptDraftRequest requestWithOneItem() {
        return new SaveImportReceiptDraftRequest(1L, 10L, "Phieu nhap du kien", List.of(item(25L)));
    }

    private SaveImportReceiptDraftItemRequest item(Long productId) {
        return new SaveImportReceiptDraftItemRequest(productId, 10, new BigDecimal("125000"), "Lo 1");
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

    private ImportReceipt receipt(Long id, Employee owner, ImportReceiptStatus status) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(id);
        receipt.setCode("PNK-001");
        receipt.setCreatedBy(owner);
        receipt.setStatus(status);
        receipt.setTotalAmount(BigDecimal.ZERO);
        receipt.setVersion(0L);
        return receipt;
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

    private DataIntegrityViolationException duplicateDetailException() {
        return new DataIntegrityViolationException(
                "duplicate detail",
                new RuntimeException("duplicate key value violates unique constraint chi_tiet_phieu_nhap_phieu_nhap_id_san_pham_id_idx")
        );
    }
}
