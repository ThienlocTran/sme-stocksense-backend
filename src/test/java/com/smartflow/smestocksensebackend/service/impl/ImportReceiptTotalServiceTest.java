package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.ConflictException;
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
import org.mockito.InOrder;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptTotalServiceTest {

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
        receipt = receipt(123L, owner);
        product = product(25L);
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addItem_shouldUpdateHeaderTotalAfterSavingDetail() {
        stubValidAddItem(new BigDecimal("150.00"));

        ImportReceiptItemResponse response = importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 2, new BigDecimal("50.00"), null)
        );

        assertEquals(new BigDecimal("100.00"), response.lineTotal());
        assertEquals(new BigDecimal("150.00"), receipt.getTotalAmount());

        InOrder inOrder = inOrder(importReceiptDetailRepository, importReceiptRepository);
        inOrder.verify(importReceiptDetailRepository).saveAndFlush(any(ImportReceiptDetail.class));
        inOrder.verify(importReceiptDetailRepository).sumLineTotalByReceiptId(123L);
        inOrder.verify(importReceiptRepository).saveAndFlush(receipt);
    }

    @Test
    void addItem_withMultipleLines_shouldUseAggregatedReceiptTotal() {
        stubValidAddItem(new BigDecimal("300.00"));

        importReceiptService.addItem(123L, new AddImportReceiptItemRequest(25L, 2, new BigDecimal("50.00"), null));

        assertEquals(new BigDecimal("300.00"), receipt.getTotalAmount());
        verify(importReceiptDetailRepository).sumLineTotalByReceiptId(123L);
    }

    @Test
    void addItem_shouldNotAddNewLineTotalToOldHeaderTotal() {
        receipt.setTotalAmount(new BigDecimal("999.00"));
        stubValidAddItem(new BigDecimal("300.00"));

        importReceiptService.addItem(123L, new AddImportReceiptItemRequest(25L, 2, new BigDecimal("50.00"), null));

        assertEquals(new BigDecimal("300.00"), receipt.getTotalAmount());
    }

    @Test
    void addItem_whenDetailSaveFails_shouldNotUpdateHeader() {
        receipt.setTotalAmount(new BigDecimal("10.00"));
        stubReceiptAndProduct();
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class)))
                .thenThrow(new DataIntegrityViolationException("detail failed"));

        assertThrows(DataIntegrityViolationException.class, () -> importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 2, new BigDecimal("50.00"), null)
        ));

        assertEquals(new BigDecimal("10.00"), receipt.getTotalAmount());
        verify(importReceiptDetailRepository, never()).sumLineTotalByReceiptId(123L);
        verify(importReceiptRepository, never()).saveAndFlush(any(ImportReceipt.class));
    }

    @Test
    void addItem_whenHeaderSaveFails_shouldPropagateForRollback() {
        stubReceiptAndProduct();
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("100.00"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenThrow(new DataIntegrityViolationException("header failed"));

        assertThrows(DataIntegrityViolationException.class, () -> importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 2, new BigDecimal("50.00"), null)
        ));

        verify(importReceiptRepository).saveAndFlush(receipt);
    }

    @Test
    void addItem_whenHeaderVersionConflicts_shouldReturnConflict() {
        stubReceiptAndProduct();
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("100.00"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenThrow(new OptimisticLockingFailureException("version conflict") {
        });

        assertThrows(ConflictException.class, () -> importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 2, new BigDecimal("50.00"), null)
        ));
    }

    @Test
    void requestDto_shouldNotAcceptClientTotals() {
        List<String> fields = Arrays.stream(AddImportReceiptItemRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of("productId", "quantity", "unitPrice", "note"), fields);
        assertFalse(fields.contains("lineTotal"));
        assertFalse(fields.contains("totalAmount"));
    }

    @Test
    void addItemContract_shouldKeepT77ResponseShape() {
        List<String> fields = Arrays.stream(ImportReceiptItemResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(List.of(
                "id",
                "receiptId",
                "productId",
                "productCode",
                "productName",
                "quantity",
                "unitPrice",
                "lineTotal",
                "note"
        ), fields);
    }

    private void stubValidAddItem(BigDecimal aggregatedTotal) {
        stubReceiptAndProduct();
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(aggregatedTotal);
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);
    }

    private void stubReceiptAndProduct() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);
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
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.HOAT_DONG);
        return employee;
    }

    private ImportReceipt receipt(Long id, Employee owner) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(id);
        receipt.setCreatedBy(owner);
        receipt.setStatus(ImportReceiptStatus.NHAP);
        receipt.setTotalAmount(BigDecimal.ZERO);
        receipt.setVersion(0L);
        return receipt;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setCode("SP-001");
        product.setName("Ca phe rang xay");
        product.setStatus(ProductStatus.HOAT_DONG);
        return product;
    }
}
