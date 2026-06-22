package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptDetailServiceTest {

    @Mock
    private ImportReceiptRepository importReceiptRepository;

    @Mock
    private ImportReceiptDetailRepository importReceiptDetailRepository;

    @InjectMocks
    private ImportReceiptServiceImpl importReceiptService;

    @Test
    void getDetail_ownerEmployeeShouldReturnDetail() {
        Employee owner = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(owner);

        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.NHAP, owner);
        List<ImportReceiptDetail> details = List.of(createDetail(1L, receipt));

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(details);

        ImportReceiptDraftResponse response = importReceiptService.getDetail(100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo("NHAP");
        assertThat(response.details()).hasSize(1);
        assertThat(response.detailCount()).isEqualTo(1);
    }

    @Test
    void getDetail_adminShouldReadAnyReceipt() {
        Employee admin = createEmployee(2L, RoleCode.ADMIN, EmployeeStatus.HOAT_DONG);
        authenticateAs(admin);

        Employee otherEmployee = createEmployee(3L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.NHAP, otherEmployee);
        List<ImportReceiptDetail> details = List.of(createDetail(1L, receipt));

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(details);

        ImportReceiptDraftResponse response = importReceiptService.getDetail(100L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    void getDetail_nonOwnerEmployeeShouldThrowMissingRoleException() {
        Employee employee = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(employee);

        Employee owner = createEmployee(2L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.NHAP, owner);

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> importReceiptService.getDetail(100L))
                .isInstanceOf(MissingRoleException.class)
                .hasMessageContaining("Khong co quyen xem phieu nhap cua nguoi khac.");
    }

    @Test
    void getDetail_missingReceiptShouldThrowNotFoundException() {
        Employee employee = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(employee);

        when(importReceiptRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importReceiptService.getDetail(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Phieu nhap khong ton tai.");
    }

    @Test
    void getDetail_inactiveEmployeeShouldThrowAccountInactiveException() {
        Employee inactive = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.TAM_KHOA);
        authenticateAs(inactive);

        assertThatThrownBy(() -> importReceiptService.getDetail(100L))
                .isInstanceOf(AccountInactiveException.class);
    }

    @Test
    void getDetail_managerShouldThrowMissingRoleException() {
        Employee manager = createEmployee(1L, RoleCode.MANAGER, EmployeeStatus.HOAT_DONG);
        authenticateAs(manager);

        assertThatThrownBy(() -> importReceiptService.getDetail(100L))
                .isInstanceOf(MissingRoleException.class)
                .hasMessageContaining("Khong co quyen xem danh sach phieu nhap ca nhan.");
    }

    @Test
    void getDetail_shouldNotMutateData() {
        Employee owner = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(owner);

        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.NHAP, owner);
        List<ImportReceiptDetail> details = List.of(createDetail(1L, receipt));

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(100L)).thenReturn(details);

        ImportReceiptDraftResponse response = importReceiptService.getDetail(100L);

        assertThat(receipt.getStatus()).isEqualTo(ImportReceiptStatus.NHAP);
        assertThat(receipt.getTotalAmount()).isEqualTo(BigDecimal.valueOf(1000));
    }

    @Test
    void inspectReceipt_success_whenMatched() {
        Employee owner = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(owner);

        Partner supplier = new Partner();
        supplier.setId(10L);
        supplier.setType(PartnerType.NHA_CUNG_CAP);
        supplier.setStatus(PartnerStatus.HOAT_DONG);

        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.CHO_KIEM_HANG, owner);
        receipt.setSupplier(supplier);

        Product product = new Product();
        product.setId(20L);
        product.setCode("SP-20");
        product.setName("Product 20");

        ImportReceiptDetail detail = createDetail(1L, receipt);
        detail.setProduct(product);
        detail.setExpectedQuantity(10);

        List<ImportReceiptDetail> details = List.of(detail);

        LocalDateTime now = LocalDateTime.now();
        InspectImportReceiptItemRequest itemRequest = new InspectImportReceiptItemRequest(20L, 10, "Binh thuong", now);
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(List.of(itemRequest));

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(100L)).thenReturn(details);
        when(importReceiptDetailRepository.saveAllAndFlush(details)).thenReturn(details);
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        ImportReceiptDraftResponse response = importReceiptService.inspectReceipt(100L, request);

        assertThat(response).isNotNull();
        assertThat(detail.getActualReceivedQuantity()).isEqualTo(10);
        assertThat(detail.getPhysicalStatus()).isEqualTo("Binh thuong");
        assertThat(detail.getExpiryDate()).isEqualTo(now);
        assertThat(detail.getRowStatus()).isEqualTo("KHOP");
    }

    @Test
    void inspectReceipt_success_whenMismatched() {
        Employee owner = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(owner);

        Partner supplier = new Partner();
        supplier.setId(10L);
        supplier.setType(PartnerType.NHA_CUNG_CAP);
        supplier.setStatus(PartnerStatus.HOAT_DONG);

        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.CHO_KIEM_HANG, owner);
        receipt.setSupplier(supplier);

        Product product = new Product();
        product.setId(20L);
        product.setCode("SP-20");
        product.setName("Product 20");

        ImportReceiptDetail detail = createDetail(1L, receipt);
        detail.setProduct(product);
        detail.setExpectedQuantity(10);

        List<ImportReceiptDetail> details = List.of(detail);

        LocalDateTime now = LocalDateTime.now();
        InspectImportReceiptItemRequest itemRequest = new InspectImportReceiptItemRequest(20L, 8, "Binh thuong", now);
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(List.of(itemRequest));

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));
        when(importReceiptDetailRepository.findByDocumentId(100L)).thenReturn(details);
        when(importReceiptDetailRepository.saveAllAndFlush(details)).thenReturn(details);
        when(importReceiptRepository.saveAndFlush(receipt)).thenReturn(receipt);

        ImportReceiptDraftResponse response = importReceiptService.inspectReceipt(100L, request);

        assertThat(response).isNotNull();
        assertThat(detail.getActualReceivedQuantity()).isEqualTo(8);
        assertThat(detail.getPhysicalStatus()).isEqualTo("Binh thuong");
        assertThat(detail.getExpiryDate()).isEqualTo(now);
        assertThat(detail.getRowStatus()).isEqualTo("CHENH_LECH");
    }

    @Test
    void inspectReceipt_error_whenStatusNotChoKiemHang() {
        Employee owner = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(owner);

        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.NHAP, owner);
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(Collections.emptyList());

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> importReceiptService.inspectReceipt(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Phieu nhap khong o trang thai CHO_KIEM_HANG.");
    }

    @Test
    void inspectReceipt_error_whenPartnerNotSupplier() {
        Employee owner = createEmployee(1L, RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        authenticateAs(owner);

        Partner supplier = new Partner();
        supplier.setId(10L);
        supplier.setType(PartnerType.KHACH_HANG);
        supplier.setStatus(PartnerStatus.HOAT_DONG);

        ImportReceipt receipt = createReceipt(100L, ImportReceiptStatus.CHO_KIEM_HANG, owner);
        receipt.setSupplier(supplier);

        InspectImportReceiptRequest request = new InspectImportReceiptRequest(Collections.emptyList());

        when(importReceiptRepository.findById(100L)).thenReturn(Optional.of(receipt));

        assertThatThrownBy(() -> importReceiptService.inspectReceipt(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chi ap dung kiem hang cho phieu nhap tu nha cung cap.");
    }

    private Employee createEmployee(Long id, RoleCode code, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmail("employee" + id + "@test.com");
        employee.setFullName("Employee " + id);
        employee.setStatus(status);

        Role role = new Role();
        role.setCode(code);
        employee.setRole(role);

        return employee;
    }

    private ImportReceipt createReceipt(Long id, ImportReceiptStatus status, Employee creator) {
        ImportReceipt receipt = new ImportReceipt();
        receipt.setId(id);
        receipt.setCode("PNK-" + id);
        receipt.setStatus(status);
        receipt.setCreatedBy(creator);
        receipt.setTotalAmount(BigDecimal.valueOf(1000));
        return receipt;
    }

    private ImportReceiptDetail createDetail(Long id, ImportReceipt receipt) {
        ImportReceiptDetail detail = new ImportReceiptDetail();
        detail.setId(id);
        detail.setDocument(receipt);
        detail.setExpectedQuantity(10);
        detail.setExpectedUnitPrice(BigDecimal.valueOf(100));
        detail.setExpectedLineTotal(BigDecimal.valueOf(1000));
        return detail;
    }

    private void authenticateAs(Employee employee) {
        org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + employee.getRole().getCode().name());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(employee, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
