package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptItemServiceTest {

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
    private ImportReceiptAmountCalculator amountCalculator;
    private ImportReceiptItemValidator itemValidator;
    private Employee owner;
    private ImportReceipt receipt;
    private Product product;

    @BeforeEach
    void setUp() {
        itemValidator = new ImportReceiptItemValidator(productRepository, importReceiptDetailRepository);
        amountCalculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);
        importReceiptService = new ImportReceiptServiceImpl(
                importReceiptRepository,
                importReceiptDetailRepository,
                warehouseRepository,
                partnerRepository,
                codeGenerator,
                itemValidator,
                amountCalculator,
                null,
                null
        );
        owner = employee(5L, RoleCode.EMPLOYEE);
        receipt = receipt(123L, owner, ImportReceiptStatus.NHAP);
        product = product(25L, ProductStatus.HOAT_DONG);
        authenticate(owner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addItem_shouldSaveValidProductLineInDraftReceipt() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class))).thenAnswer(invocation -> {
            ImportReceiptDetail detail = invocation.getArgument(0);
            detail.setId(1001L);
            return detail;
        });
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("1250000"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        ImportReceiptItemResponse response = importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 10, new BigDecimal("125000"), "  Lo hang thang 6  ")
        );

        assertEquals(1001L, response.id());
        assertEquals(123L, response.receiptId());
        assertEquals(25L, response.productId());
        assertEquals("SP-001", response.productCode());
        assertEquals("Ca phe rang xay", response.productName());
        assertEquals(10, response.quantity());
        assertEquals(new BigDecimal("125000"), response.unitPrice());
        assertEquals(new BigDecimal("1250000"), response.lineTotal());
        assertEquals("Lo hang thang 6", response.note());

        ArgumentCaptor<ImportReceiptDetail> captor = ArgumentCaptor.forClass(ImportReceiptDetail.class);
        verify(importReceiptDetailRepository).saveAndFlush(captor.capture());
        ImportReceiptDetail saved = captor.getValue();
        assertEquals(receipt, saved.getDocument());
        assertEquals(product, saved.getProduct());
        assertEquals(10, saved.getExpectedQuantity());
        assertEquals(new BigDecimal("125000"), saved.getExpectedUnitPrice());
        assertEquals(new BigDecimal("1250000"), saved.getExpectedLineTotal());
        assertNull(saved.getActualReceivedQuantity());
    }

    @Test
    void addItem_shouldCalculateLineTotalInBackend() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("58.50"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        ImportReceiptItemResponse response = importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 3, new BigDecimal("19.50"), null)
        );

        assertEquals(new BigDecimal("58.50"), response.lineTotal());
    }

    @Test
    void addItem_withMissingReceipt_shouldThrowNotFoundException() {
        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.addItem(
                404L,
                validRequest()
        ));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withNonDraftReceipt_shouldThrowConflictException() {
        receipt.setStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(ConflictException.class, () -> importReceiptService.addItem(123L, validRequest()));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withDifferentOwnerEmployee_shouldThrowMissingRoleException() {
        authenticate(employee(6L, RoleCode.EMPLOYEE));
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> importReceiptService.addItem(123L, validRequest()));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withMissingProduct_shouldThrowNotFoundException() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.addItem(123L, validRequest()));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withInactiveProduct_shouldThrowBadRequestException() {
        product.setStatus(ProductStatus.NGUNG_HOAT_DONG);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> importReceiptService.addItem(123L, validRequest()));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withDuplicateProduct_shouldThrowConflictException() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> importReceiptService.addItem(123L, validRequest()));
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withDatabaseDuplicateProductRace_shouldThrowConflictException() {
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint "
                                + "chi_tiet_phieu_nhap_phieu_nhap_id_san_pham_id_idx"
                ));

        assertThrows(ConflictException.class, () -> importReceiptService.addItem(123L, validRequest()));
    }

    @Test
    void addItem_withNonPositiveQuantity_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 0, BigDecimal.ONE, null)
        ));
        verify(importReceiptRepository, never()).findById(any());
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_withNegativeUnitPrice_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> importReceiptService.addItem(
                123L,
                new AddImportReceiptItemRequest(25L, 1, new BigDecimal("-1"), null)
        ));
        verify(importReceiptRepository, never()).findById(any());
        verify(importReceiptDetailRepository, never()).saveAndFlush(any());
    }

    @Test
    void addItem_shouldUpdateReceiptTotalWithoutChangingStatus() {
        receipt.setTotalAmount(BigDecimal.ZERO);
        when(importReceiptRepository.findById(123L)).thenReturn(Optional.of(receipt));
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);
        when(importReceiptDetailRepository.saveAndFlush(any(ImportReceiptDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("1250000"));
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        importReceiptService.addItem(123L, validRequest());

        assertEquals(ImportReceiptStatus.NHAP, receipt.getStatus());
        assertEquals(new BigDecimal("1250000"), receipt.getTotalAmount());
        verify(importReceiptRepository).saveAndFlush(receipt);
    }

    private AddImportReceiptItemRequest validRequest() {
        return new AddImportReceiptItemRequest(25L, 10, new BigDecimal("125000"), "Lo hang thang 6");
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
        receipt.setCreatedBy(owner);
        receipt.setStatus(status);
        return receipt;
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
