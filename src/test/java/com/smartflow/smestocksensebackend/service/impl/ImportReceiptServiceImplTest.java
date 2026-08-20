package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptArrivalRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptCodeGenerator;
import com.smartflow.smestocksensebackend.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptServiceImplTest {

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
    private ImportReceiptAmountCalculator amountCalculator;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private com.smartflow.smestocksensebackend.repository.SystemSettingRepository systemSettingRepository;

    @Mock
    private com.smartflow.smestocksensebackend.service.WarehouseCapacityService warehouseCapacityService;

    @Mock
    private com.smartflow.smestocksensebackend.service.EmailService emailService;

    @InjectMocks
    private ImportReceiptServiceImpl importReceiptService;

    private Employee creator;
    private Warehouse warehouse;
    private Partner supplier;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(systemSettingRepository.findById("IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD")).thenReturn(java.util.Optional.empty());
        creator = new Employee();
        creator.setId(5L);
        creator.setFullName("Nguyen Van A");
        creator.setStatus(EmployeeStatus.HOAT_DONG);

        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Kho tong");
        warehouse.setStatus(WarehouseStatus.HOAT_DONG);

        supplier = new Partner();
        supplier.setId(10L);
        supplier.setName("Nha cung cap A");
        supplier.setType(PartnerType.NHA_CUNG_CAP);
        supplier.setStatus(PartnerStatus.HOAT_DONG);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(creator, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDraft_shouldCreateDraftReceiptWithBackendControlledFields() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(codeGenerator.generate()).thenReturn("PNK-20260618-ABC123DEF456");
        when(importReceiptRepository.existsByCodeIgnoreCase("PNK-20260618-ABC123DEF456")).thenReturn(false);
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class))).thenAnswer(invocation -> {
            ImportReceipt receipt = invocation.getArgument(0);
            receipt.setId(123L);
            receipt.setVersion(0L);
            return receipt;
        });

        ImportReceiptResponse response = importReceiptService.createDraft(
                new CreateImportReceiptRequest(1L, 10L, "  Phieu nhap du kien  "));

        assertEquals(123L, response.id());
        assertEquals("PNK-20260618-ABC123DEF456", response.code());
        assertEquals(1L, response.warehouseId());
        assertEquals("Kho tong", response.warehouseName());
        assertEquals(10L, response.supplierId());
        assertEquals("Nha cung cap A", response.supplierName());
        assertEquals(5L, response.createdById());
        assertEquals("Nguyen Van A", response.createdByName());
        assertEquals("NHAP", response.status());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
        assertEquals("Phieu nhap du kien", response.note());
        assertEquals(0L, response.version());

        ArgumentCaptor<ImportReceipt> captor = ArgumentCaptor.forClass(ImportReceipt.class);
        verify(importReceiptRepository).saveAndFlush(captor.capture());
        ImportReceipt saved = captor.getValue();
        assertEquals(ImportReceiptStatus.NHAP, saved.getStatus());
        assertEquals(BigDecimal.ZERO, saved.getTotalAmount());
        assertEquals(creator, saved.getCreatedBy());
        assertEquals(warehouse, saved.getWarehouse());
        assertEquals(supplier, saved.getSupplier());
        assertNotNull(saved.getCode());
    }

    @Test
    void createDraft_withMissingWarehouse_shouldThrowNotFoundException() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.createDraft(
                new CreateImportReceiptRequest(99L, 10L, null)));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_withInactiveWarehouse_shouldThrowBadRequestException() {
        warehouse.setStatus(WarehouseStatus.NGUNG_HOAT_DONG);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        assertThrows(BadRequestException.class, () -> importReceiptService.createDraft(
                new CreateImportReceiptRequest(1L, 10L, null)));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_withMissingSupplier_shouldThrowNotFoundException() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> importReceiptService.createDraft(
                new CreateImportReceiptRequest(1L, 99L, null)));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_withInactiveSupplier_shouldThrowBadRequestException() {
        supplier.setStatus(PartnerStatus.NGUNG_HOAT_DONG);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));

        assertThrows(BadRequestException.class, () -> importReceiptService.createDraft(
                new CreateImportReceiptRequest(1L, 10L, null)));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_withCustomerPartner_shouldThrowBadRequestException() {
        supplier.setType(PartnerType.KHACH_HANG);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));

        assertThrows(BadRequestException.class, () -> importReceiptService.createDraft(
                new CreateImportReceiptRequest(1L, 10L, null)));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_shouldRetryWhenGeneratedCodeAlreadyExists() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(codeGenerator.generate()).thenReturn("PNK-DUP", "PNK-OK");
        when(importReceiptRepository.existsByCodeIgnoreCase("PNK-DUP")).thenReturn(true);
        when(importReceiptRepository.existsByCodeIgnoreCase("PNK-OK")).thenReturn(false);
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportReceiptResponse response = importReceiptService
                .createDraft(new CreateImportReceiptRequest(1L, 10L, null));

        assertEquals("PNK-OK", response.code());
    }

    @Test
    void createDraft_shouldReturnConflictWhenCodeCannotBeGenerated() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(partnerRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(codeGenerator.generate()).thenReturn("PNK-DUP");
        when(importReceiptRepository.existsByCodeIgnoreCase("PNK-DUP")).thenReturn(true);

        assertThrows(ConflictException.class, () -> importReceiptService.createDraft(
                new CreateImportReceiptRequest(1L, 10L, null)));
    }

    @Test
    void requestDto_shouldNotExposeStatusOrCreatorFields() {
        List<String> fields = List.of(CreateImportReceiptRequest.class.getRecordComponents())
                .stream()
                .map(component -> component.getName())
                .toList();

        assertFalse(fields.contains("status"));
        assertFalse(fields.contains("createdById"));
    }

    @Test
    void recordArrival_withValidStatusAndRole_shouldUpdateStatusAndArrivalDate() {
        // Arrange
        Role role = new Role();
        role.setCode(RoleCode.EMPLOYEE);
        creator.setRole(role);

        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(100L);
        receipt.setCode("PNK-20260618-SUCCESS");
        receipt.setStatus(ImportReceiptStatus.CHO_HANG_VE);
        receipt.setVersion(1L);
        receipt.setCreatedBy(creator); // EMPLOYEE phải là người tạo mới được ghi nhận hàng về

        java.time.LocalDateTime arrivalTime = java.time.LocalDateTime.of(2026, 6, 22, 10, 0);
        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(arrivalTime);

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        // Act
        ImportReceiptDraftResponse response = importReceiptService.recordArrival(100L, request);

        // Assert
        assertNotNull(response);
        assertEquals(ImportReceiptStatus.CHO_KIEM_HANG.name(), response.status());
        assertEquals(arrivalTime, receipt.getActualArrivalDate());
        // Kiểm tra mapping trong ImportReceiptDraftResponse.from() có trả đúng actualArrivalDate
        assertEquals(arrivalTime, response.actualArrivalDate());
        verify(importReceiptRepository).saveAndFlush(receipt);
    }

    @Test
    void recordArrival_withAdminRole_shouldUpdateStatusAndArrivalDate() {
        // Arrange - ADMIN cũng có quyền ghi nhận hàng về, tương tự EMPLOYEE
        Role role = new Role();
        role.setCode(RoleCode.ADMIN);
        creator.setRole(role);

        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(100L);
        receipt.setCode("PNK-20260618-ADMIN");
        receipt.setStatus(ImportReceiptStatus.CHO_HANG_VE);
        receipt.setVersion(1L);

        java.time.LocalDateTime arrivalTime = java.time.LocalDateTime.of(2026, 6, 22, 10, 0);
        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(arrivalTime);

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(List.of());

        // Act
        ImportReceiptDraftResponse response = importReceiptService.recordArrival(100L, request);

        // Assert
        assertNotNull(response);
        assertEquals(ImportReceiptStatus.CHO_KIEM_HANG.name(), response.status());
        assertEquals(arrivalTime, receipt.getActualArrivalDate());
        // Kiểm tra mapping trong ImportReceiptDraftResponse.from() có trả đúng actualArrivalDate
        assertEquals(arrivalTime, response.actualArrivalDate());
        verify(importReceiptRepository).saveAndFlush(receipt);
    }

    @Test
    void recordArrival_withInvalidStatus_shouldThrowConflictException() {
        // Arrange
        Role role = new Role();
        role.setCode(RoleCode.EMPLOYEE);
        creator.setRole(role);

        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(100L);
        receipt.setStatus(ImportReceiptStatus.NHAP); // Không phải CHO_HANG_VE

        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(java.time.LocalDateTime.now());

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        // Act & Assert
        assertThrows(ConflictException.class, () -> importReceiptService.recordArrival(100L, request));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordArrival_withInactiveEmployee_shouldThrowAccountInactiveException() {
        // Arrange
        creator.setStatus(EmployeeStatus.TAM_KHOA); // Tài khoản bị khoá

        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(java.time.LocalDateTime.now());

        // Act & Assert
        assertThrows(AccountInactiveException.class, () -> importReceiptService.recordArrival(100L, request));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordArrival_withMissingRole_shouldThrowMissingRoleException() {
        // Arrange
        Role role = new Role();
        role.setCode(RoleCode.MANAGER); // Role MANAGER không có quyền ghi nhận hàng về
        creator.setRole(role);

        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(java.time.LocalDateTime.now());

        // Act & Assert
        assertThrows(MissingRoleException.class, () -> importReceiptService.recordArrival(100L, request));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordArrival_withNotFoundReceipt_shouldThrowNotFoundException() {
        // Arrange
        Role role = new Role();
        role.setCode(RoleCode.EMPLOYEE);
        creator.setRole(role);

        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(java.time.LocalDateTime.now());

        when(importReceiptRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> importReceiptService.recordArrival(999L, request));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordArrival_withNullRole_shouldThrowMissingRoleException() {
        // Arrange - role null không có quyền ghi nhận hàng về
        creator.setRole(null);

        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(java.time.LocalDateTime.now());

        // Act & Assert
        assertThrows(MissingRoleException.class, () -> importReceiptService.recordArrival(100L, request));
        verify(importReceiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void recordArrival_withConcurrentUpdate_shouldThrowConflictException() {
        // Arrange - mô phỏng kịch bản 2 phiên cùng cập nhật đồng thời (optimistic
        // locking)
        Role role = new Role();
        role.setCode(RoleCode.EMPLOYEE);
        creator.setRole(role);

        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(100L);
        receipt.setCode("PNK-20260618-CONCURRENT");
        receipt.setStatus(ImportReceiptStatus.CHO_HANG_VE);
        receipt.setVersion(1L);
        receipt.setCreatedBy(creator); // EMPLOYEE phải là người tạo

        ImportReceiptArrivalRequest request = new ImportReceiptArrivalRequest(
                java.time.LocalDateTime.of(2026, 6, 22, 10, 0));

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        // Giả lập DB ném ra OptimisticLockingFailureException khi version bị xung đột
        when(importReceiptRepository.saveAndFlush(any(ImportReceipt.class)))
                .thenThrow(new OptimisticLockingFailureException("Version mismatch"));

        // Act & Assert
        ConflictException thrown = assertThrows(ConflictException.class,
                () -> importReceiptService.recordArrival(100L, request));

        // Kiểm tra message khớp với thông báo được định nghĩa trong service
        org.junit.jupiter.api.Assertions.assertTrue(
                thrown.getMessage().contains("được cập nhật bởi một phiên làm việc khác"),
                "Expected message about concurrent update but got: " + thrown.getMessage());
    }
}
