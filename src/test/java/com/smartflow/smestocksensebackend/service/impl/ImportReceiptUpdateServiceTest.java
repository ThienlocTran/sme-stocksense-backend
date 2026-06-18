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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptUpdateServiceTest {

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
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void update_ownerCanEditDraftReceipt() {
        ImportReceipt receipt = stubEditableReceipt(ImportReceiptStatus.NHAP);

        ImportReceiptDraftResponse response = importReceiptService.updateEditable(123L, requestWithOneItem());

        assertEquals("NHAP", response.status());
        assertEquals(new BigDecimal("1250000.00"), receipt.getTotalAmount());
        assertEquals(new BigDecimal("1250000"), response.details().getFirst().lineTotal());
    }

    @Test
    void update_ownerCanEditRejectedReceiptAndKeepRejectedStatusAndReason() {
        ImportReceipt receipt = stubEditableReceipt(ImportReceiptStatus.TU_CHOI);
        receipt.setRejectionReason("Thieu bao gia");

        ImportReceiptDraftResponse response = importReceiptService.updateEditable(123L, requestWithOneItem());

        assertEquals("TU_CHOI", response.status());
        assertEquals(ImportReceiptStatus.TU_CHOI, receipt.getStatus());
        assertEquals("Thieu bao gia", receipt.getRejectionReason());
    }

    @Test
    void update_adminCanEditDraftReceipt() {
        authenticate(employee(1L, RoleCode.ADMIN));
        stubHeaderOnlyReceipt(ImportReceiptStatus.NHAP);

        ImportReceiptDraftResponse response = importReceiptService.updateEditable(123L, minimalRequest());

        assertEquals("NHAP", response.status());
    }

    @Test
    void update_adminCanEditRejectedReceipt() {
        authenticate(employee(1L, RoleCode.ADMIN));
        stubHeaderOnlyReceipt(ImportReceiptStatus.TU_CHOI);

        ImportReceiptDraftResponse response = importReceiptService.updateEditable(123L, minimalRequest());

        assertEquals("TU_CHOI", response.status());
    }

    @Test
    void update_employeeCannotEditOtherEmployeeReceipt() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        authenticate(employee(6L, RoleCode.EMPLOYEE));

        assertThrows(MissingRoleException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));
    }

    @Test
    void update_managerCannotEditEmployeeReceipt() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        authenticate(employee(7L, RoleCode.MANAGER));

        assertThrows(MissingRoleException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));
    }

    @Test
    void update_missingReceiptShouldThrowNotFound() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.updateEditable(404L, minimalRequest()));
    }

    @Test
    void update_nonEditableStatusesShouldThrowConflict() {
        for (ImportReceiptStatus status : List.of(
                ImportReceiptStatus.CHO_DUYET_CAP_1,
                ImportReceiptStatus.CHO_DUYET_CAP_2,
                ImportReceiptStatus.CHO_HANG_VE,
                ImportReceiptStatus.CHO_KIEM_HANG,
                ImportReceiptStatus.HUY,
                ImportReceiptStatus.HOAN_THANH
        )) {
            ImportReceipt receipt = receipt(123L, owner, status);
            when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

            assertThrows(ConflictException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));
        }
    }

    @Test
    void update_invalidWarehouseShouldThrowBeforeMutation() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.TU_CHOI);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));

        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void update_inactiveWarehouseShouldThrow() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, WarehouseStatus.NGUNG_HOAT_DONG)));

        assertThrows(BadRequestException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));
    }

    @Test
    void update_invalidSupplierShouldThrow() {
        ImportReceipt receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));

        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, PartnerStatus.NGUNG_HOAT_DONG, PartnerType.NHA_CUNG_CAP)));
        assertThrows(BadRequestException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));

        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier(10L, PartnerStatus.HOAT_DONG, PartnerType.KHACH_HANG)));
        assertThrows(BadRequestException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));
    }

    @Test
    void update_invalidProductShouldNotDeleteOldDetails() {
        stubReceiptAndHeader(ImportReceiptStatus.NHAP);
        when(productRepository.findById(25L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.updateEditable(123L, requestWithOneItem()));

        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
        verify(importReceiptDetailRepository, never()).saveAllAndFlush(anyList());
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void update_inactiveProductShouldThrow() {
        stubReceiptAndHeader(ImportReceiptStatus.NHAP);
        when(productRepository.findById(25L)).thenReturn(Optional.of(product(25L, ProductStatus.NGUNG_HOAT_DONG)));

        assertThrows(BadRequestException.class, () -> importReceiptService.updateEditable(123L, requestWithOneItem()));
    }

    @Test
    void update_duplicateProductInPayloadShouldThrowConflict() {
        stubReceiptAndHeader(ImportReceiptStatus.NHAP);
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));

        assertThrows(ConflictException.class, () -> importReceiptService.updateEditable(123L, new SaveImportReceiptDraftRequest(
                1L,
                10L,
                null,
                List.of(item(25L, 10, "125000"), item(25L, 1, "1"))
        )));

        verify(importReceiptDetailRepository, never()).deleteByDocumentId(123L);
    }

    @Test
    void update_shouldCalculateLineTotalAndReceiptTotalOnBackend() {
        ImportReceipt receipt = stubEditableReceipt(ImportReceiptStatus.NHAP);

        importReceiptService.updateEditable(123L, new SaveImportReceiptDraftRequest(
                1L,
                10L,
                null,
                List.of(item(25L, 3, "7.50"))
        ));

        ArgumentCaptor<List<ImportReceiptDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(importReceiptDetailRepository).saveAllAndFlush(captor.capture());
        assertEquals(new BigDecimal("22.50"), captor.getValue().getFirst().getExpectedLineTotal());
        assertEquals(new BigDecimal("1250000.00"), receipt.getTotalAmount());
    }

    @Test
    void update_shouldNotChangeCodeCreatorStatusOrApprovalFields() {
        ImportReceipt receipt = stubHeaderOnlyReceipt(ImportReceiptStatus.TU_CHOI);

        importReceiptService.updateEditable(123L, minimalRequest());

        assertEquals("PNK-001", receipt.getCode());
        assertEquals(owner, receipt.getCreatedBy());
        assertEquals(ImportReceiptStatus.TU_CHOI, receipt.getStatus());
        assertEquals(null, receipt.getSubmittedBy());
        assertEquals(null, receipt.getSubmittedAt());
        assertEquals(null, receipt.getLevel1ApprovedBy());
        assertEquals(null, receipt.getLevel2ApprovedBy());
    }

    @Test
    void update_whenVersionConflictsShouldThrowConflict() {
        stubReceiptAndHeader(ImportReceiptStatus.NHAP);
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of());
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(BigDecimal.ZERO);
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenThrow(new OptimisticLockingFailureException("version"));

        assertThrows(ConflictException.class, () -> importReceiptService.updateEditable(123L, minimalRequest()));
    }

    private ImportReceipt stubEditableReceipt(ImportReceiptStatus status) {
        ImportReceipt receipt = stubReceiptAndHeader(status);
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("1250000.00"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);
        return receipt;
    }

    private ImportReceipt stubHeaderOnlyReceipt(ImportReceiptStatus status) {
        ImportReceipt receipt = stubReceiptAndHeader(status);
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of());
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(BigDecimal.ZERO);
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);
        return receipt;
    }

    private ImportReceipt stubReceiptAndHeader(ImportReceiptStatus status) {
        ImportReceipt receipt = receipt(123L, owner, status);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));
        return receipt;
    }

    private SaveImportReceiptDraftRequest minimalRequest() {
        return new SaveImportReceiptDraftRequest(1L, 10L, "Phieu nhap du kien", null);
    }

    private SaveImportReceiptDraftRequest requestWithOneItem() {
        return new SaveImportReceiptDraftRequest(1L, 10L, "Phieu nhap du kien", List.of(item(25L, 10, "125000")));
    }

    private SaveImportReceiptDraftItemRequest item(Long productId, int quantity, String unitPrice) {
        return new SaveImportReceiptDraftItemRequest(productId, quantity, new BigDecimal(unitPrice), "Lo 1");
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
}
