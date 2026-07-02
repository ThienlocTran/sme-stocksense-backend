package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.*;
import com.smartflow.smestocksensebackend.service.ImportReceiptCodeGenerator;
import com.smartflow.smestocksensebackend.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportReceiptCompleteServiceTest {

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

    @Mock
    private DiscrepancyReportRepository discrepancyReportRepository;

    @Mock
    private DiscrepancyReportDetailRepository discrepancyReportDetailRepository;

    @Mock
    private InventoryService inventoryService;

    private ImportReceiptServiceImpl importReceiptService;
    private Employee employee;
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
                amountCalculator,
                discrepancyReportRepository,
                discrepancyReportDetailRepository,
                inventoryService,
                null
        );

        employee = employee(5L, RoleCode.EMPLOYEE);
        warehouse = warehouse(1L, WarehouseStatus.HOAT_DONG);
        supplier = supplier(10L, PartnerStatus.HOAT_DONG, PartnerType.NHA_CUNG_CAP);
        product = product(25L, ProductStatus.HOAT_DONG);
        receipt = receipt(123L, employee, ImportReceiptStatus.CHO_KIEM_HANG);
        detail = detail(1001L, receipt, product, 10, new BigDecimal("125000.00"));

        authenticate(employee);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeImport_success_whenInspectionMatches() {
        detail.setExpectedQuantity(12);
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 12, "Binh thuong", null))
        );

        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(123L)).thenReturn(List.of(detail));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportReceiptDraftResponse response = importReceiptService.completeImport(123L, request);

        assertNotNull(response);
        assertEquals("HOAN_THANH", response.status());
        assertEquals(ImportReceiptStatus.HOAN_THANH, receipt.getStatus());
        assertEquals(employee, receipt.getCompletedBy());
        assertNotNull(receipt.getCompletedAt());

        ArgumentCaptor<ImportReceipt> receiptCaptor = ArgumentCaptor.forClass(ImportReceipt.class);
        verify(inventoryService, times(1)).increaseInventory(eq(25L), eq(1L), eq(12), receiptCaptor.capture());
        assertSame(receipt, receiptCaptor.getValue());
        assertEquals(ImportReceiptStatus.HOAN_THANH, receiptCaptor.getValue().getStatus());
    }

    @Test
    void completeImport_success_whenActualReceivedQuantityIsZero_shouldNotIncreaseInventory() {
        detail.setExpectedQuantity(0);
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 0, "Binh thuong", null))
        );

        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(123L)).thenReturn(List.of(detail));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportReceiptDraftResponse response = importReceiptService.completeImport(123L, request);

        assertNotNull(response);
        assertEquals("HOAN_THANH", response.status());
        // Verify inventory service is NEVER called for 0 quantity
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any(ImportReceipt.class));
    }

    @Test
    void completeImport_error_whenDiscrepancyWithoutReport_shouldThrowClearBadRequest() {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 8, "Binh thuong", null))
        );

        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(123L)).thenReturn(List.of(detail));
        when(discrepancyReportRepository.findByImportReceiptId(123L)).thenReturn(Optional.empty());
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> importReceiptService.completeImport(123L, request));

        assertEquals("Có chênh lệch số lượng/tình trạng hàng. Vui lòng lưu biên bản chênh lệch trước khi hoàn tất nhập kho.",
                exception.getMessage());
        assertEquals(ImportReceiptStatus.CHO_KIEM_HANG, receipt.getStatus());
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any(ImportReceipt.class));
        verify(importReceiptRepository, times(1)).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void completeImport_success_whenDiscrepancyReportExists_shouldUseActualQuantity() {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 8, "Binh thuong", null))
        );

        DiscrepancyReport report = new DiscrepancyReport();
        report.setId(99L);
        report.setImportReceipt(receipt);

        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(123L)).thenReturn(List.of(detail));
        when(discrepancyReportRepository.findByImportReceiptId(123L)).thenReturn(Optional.of(report));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportReceiptDraftResponse response = importReceiptService.completeImport(123L, request);

        assertNotNull(response);
        assertEquals("HOAN_THANH", response.status());
        verify(inventoryService, times(1)).increaseInventory(eq(25L), eq(1L), eq(8), same(receipt));
    }

    @Test
    void completeImport_error_whenInvalidStatus_shouldThrowBadRequest() {
        receipt.setStatus(ImportReceiptStatus.NHAP);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 10, "Binh thuong", null))
        );

        assertThrows(BadRequestException.class, () -> importReceiptService.completeImport(123L, request));
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any(ImportReceipt.class));
    }

    @Test
    void completeImport_error_whenAlreadyCompleted_shouldThrowBadRequest() {
        receipt.setStatus(ImportReceiptStatus.HOAN_THANH);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 10, "Binh thuong", null))
        );

        assertThrows(BadRequestException.class, () -> importReceiptService.completeImport(123L, request));
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any(ImportReceipt.class));
    }

    @Test
    void completeImport_error_whenInactiveEmployee_shouldThrowAccountInactiveException() {
        Employee inactiveEmployee = employee(5L, RoleCode.EMPLOYEE);
        inactiveEmployee.setStatus(EmployeeStatus.NGUNG_HOAT_DONG);
        authenticate(inactiveEmployee);

        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 10, "Binh thuong", null))
        );

        assertThrows(AccountInactiveException.class, () -> importReceiptService.completeImport(123L, request));
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any(ImportReceipt.class));
    }

    @Test
    void completeImport_error_whenManagerCalls_shouldThrowMissingRoleException() {
        authenticate(employee(7L, RoleCode.MANAGER));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(List.of(new InspectImportReceiptItemRequest(25L, 10, "Binh thuong", null)));
        assertThrows(MissingRoleException.class, () -> importReceiptService.completeImport(123L, request));
        verify(inventoryService, never()).increaseInventory(anyLong(), anyLong(), anyInt(), any(ImportReceipt.class));
    }

    @Test
    void completeImport_error_whenInventoryServiceFails_shouldPropagateExceptionForRollback() {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(25L, 10, "Binh thuong", null))
        );

        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(123L)).thenReturn(List.of(detail));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(123L)).thenReturn(List.of(detail));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        doAnswer(invocation -> {
            ImportReceipt receiptArg = invocation.getArgument(3);
            assertEquals(ImportReceiptStatus.HOAN_THANH, receiptArg.getStatus());
            throw new NotFoundException("Sản phẩm không tồn tại.");
        }).when(inventoryService).increaseInventory(25L, 1L, 10, receipt);

        assertThrows(NotFoundException.class, () -> importReceiptService.completeImport(123L, request));

        verify(importReceiptRepository, times(1)).saveAndFlush(any(ImportReceipt.class));
    }

    private void authenticate(Employee emp) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(emp, null, List.of())
        );
    }

    private Employee employee(Long id, RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee emp = new Employee();
        emp.setId(id);
        emp.setFullName("Nguyen Van A");
        emp.setRole(role);
        emp.setStatus(EmployeeStatus.HOAT_DONG);
        return emp;
    }

    private ImportReceipt receipt(Long id, Employee creator, ImportReceiptStatus status) {
        ImportReceipt rec = new ImportReceipt();
        rec.setId(id);
        rec.setCode("PNK-001");
        rec.setCreatedBy(creator);
        rec.setWarehouse(warehouse);
        rec.setSupplier(supplier);
        rec.setStatus(status);
        rec.setTotalAmount(BigDecimal.ZERO);
        rec.setVersion(1L);
        return rec;
    }

    private ImportReceiptDetail detail(Long id, ImportReceipt rec, Product prod, Integer quantity, BigDecimal unitPrice) {
        ImportReceiptDetail det = new ImportReceiptDetail();
        det.setId(id);
        det.setDocument(rec);
        det.setProduct(prod);
        det.setExpectedQuantity(quantity);
        det.setExpectedUnitPrice(unitPrice);
        det.setExpectedLineTotal(BigDecimal.ZERO);
        det.setNote("Lo 1");
        return det;
    }

    private Warehouse warehouse(Long id, WarehouseStatus status) {
        Warehouse wh = new Warehouse();
        wh.setId(id);
        wh.setName("Kho tong");
        wh.setStatus(status);
        return wh;
    }

    private Partner supplier(Long id, PartnerStatus status, PartnerType type) {
        Partner sup = new Partner();
        sup.setId(id);
        sup.setName("Nha cung cap A");
        sup.setStatus(status);
        sup.setType(type);
        return sup;
    }

    private Product product(Long id, ProductStatus status) {
        Product prod = new Product();
        prod.setId(id);
        prod.setCode("SP-001");
        prod.setName("Ca phe rang xay");
        prod.setStatus(status);
        return prod;
    }
}
