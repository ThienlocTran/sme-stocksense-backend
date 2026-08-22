package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.aiassignment.CreateAiPurchaseAssignmentRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestEmailStatus;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.AiPurchaseRequestRepository;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.ForecastModelMetadataRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AiPurchaseAssignmentServiceImplTest {

    @Mock AiPurchaseRequestRepository aiPurchaseRequestRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock ProductRepository productRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock ForecastModelMetadataRepository forecastModelMetadataRepository;

    @InjectMocks AiPurchaseAssignmentServiceImpl service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanCreateAssignment() {
        stubHappyPath(RoleCode.ADMIN);

        service.createAssignment(request());

        verify(aiPurchaseRequestRepository).saveAndFlush(any(AiPurchaseRequest.class));
    }

    @Test
    void managerCanCreateAssignment() {
        stubHappyPath(RoleCode.MANAGER);

        service.createAssignment(request());

        verify(aiPurchaseRequestRepository).saveAndFlush(any(AiPurchaseRequest.class));
    }

    @Test
    void employeeCannotCreateAssignment() {
        Employee sender = employee(1L, RoleCode.EMPLOYEE);
        authenticate(sender);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sender));

        assertThrows(MissingRoleException.class, () -> service.createAssignment(request()));

        verify(aiPurchaseRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void senderComesFromAuthenticationContext() {
        stubHappyPath(RoleCode.MANAGER);

        service.createAssignment(request());

        AiPurchaseRequest saved = savedAssignment();
        assertEquals(1L, saved.getSender().getId());
    }

    @Test
    void selectedEmployeeAndIndependentQuantitiesPersisted() {
        stubHappyPath(RoleCode.MANAGER);

        service.createAssignment(request());

        AiPurchaseRequest saved = savedAssignment();
        assertEquals(2L, saved.getReceiver().getId());
        assertEquals(70, saved.getAiSuggestedQuantity());
        assertEquals(50, saved.getRequestedQuantity());
    }

    @Test
    void productWarehouseHorizonPersistedAndNoReceiptCreated() {
        stubHappyPath(RoleCode.MANAGER);

        service.createAssignment(request());

        AiPurchaseRequest saved = savedAssignment();
        assertEquals(10L, saved.getProduct().getId());
        assertEquals(20L, saved.getWarehouse().getId());
        assertEquals((short) 7, saved.getHorizonDays());
        assertNull(saved.getImportReceipt());
        assertEquals(AiPurchaseRequestEmailStatus.CHO_GUI, saved.getEmailStatus());
    }

    @Test
    void noInventoryOrSalesHistoryCollaboratorsExist() {
        assertThrows(NoSuchFieldException.class,
                () -> AiPurchaseAssignmentServiceImpl.class.getDeclaredField("inventoryRepository"));
        assertThrows(NoSuchFieldException.class,
                () -> AiPurchaseAssignmentServiceImpl.class.getDeclaredField("inventoryTransactionRepository"));
        assertThrows(NoSuchFieldException.class,
                () -> AiPurchaseAssignmentServiceImpl.class.getDeclaredField("salesHistoryRepository"));
    }

    @Test
    void receiverMustBeEmployee() {
        Product product = product(10L);
        Warehouse warehouse = warehouse(20L);
        Employee sender = employee(1L, RoleCode.MANAGER);
        Employee receiver = employee(2L, RoleCode.MANAGER);
        authenticate(sender);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(receiver));

        assertThrows(BadRequestException.class, () -> service.createAssignment(request()));

        verify(aiPurchaseRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    void employeeSeesOwnAssignments() {
        Employee actor = employee(2L, RoleCode.EMPLOYEE);
        authenticate(actor);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(actor));
        when(aiPurchaseRequestRepository.findByReceiverId(2L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(assignment(actor))));

        var page = service.listMyAssignments(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        verify(aiPurchaseRequestRepository).findByReceiverId(2L, PageRequest.of(0, 10));
    }

    @Test
    void employeeCannotReadAnotherEmployeesAssignment() {
        Employee actor = employee(3L, RoleCode.EMPLOYEE);
        authenticate(actor);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(actor));
        when(aiPurchaseRequestRepository.findById(99L)).thenReturn(Optional.of(assignment(employee(2L, RoleCode.EMPLOYEE))));

        assertThrows(ConflictException.class, () -> service.getAssignment(99L));
    }

    @Test
    void managerCanReadAssignments() {
        Employee actor = employee(1L, RoleCode.MANAGER);
        authenticate(actor);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(aiPurchaseRequestRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(assignment(employee(2L, RoleCode.EMPLOYEE)))));

        var page = service.listMyAssignments(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        verify(aiPurchaseRequestRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    void detailResponseShowsBothQuantitiesAndNoSensitiveData() {
        Employee actor = employee(1L, RoleCode.ADMIN);
        authenticate(actor);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(aiPurchaseRequestRepository.findById(99L)).thenReturn(Optional.of(assignment(employee(2L, RoleCode.EMPLOYEE))));

        var response = service.getAssignment(99L);

        assertEquals(70, response.aiSuggestedQuantity());
        assertEquals(50, response.requestedQuantity());
        assertEquals("SP001", response.productCode());
        assertEquals("Kho chinh", response.warehouseName());
    }

    private void stubHappyPath(RoleCode senderRole) {
        stubRefs(senderRole, RoleCode.EMPLOYEE);
        when(aiPurchaseRequestRepository.saveAndFlush(any(AiPurchaseRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubRefs(RoleCode senderRole, RoleCode receiverRole) {
        Product product = product(10L);
        Warehouse warehouse = warehouse(20L);
        ForecastModelMetadata metadata = metadata(30L, product, warehouse);
        Employee sender = employee(1L, senderRole);
        Employee receiver = employee(2L, receiverRole);
        authenticate(sender);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(forecastModelMetadataRepository.findById(30L)).thenReturn(Optional.of(metadata));
    }

    private AiPurchaseRequest savedAssignment() {
        ArgumentCaptor<AiPurchaseRequest> captor = ArgumentCaptor.forClass(AiPurchaseRequest.class);
        verify(aiPurchaseRequestRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private CreateAiPurchaseAssignmentRequest request() {
        return new CreateAiPurchaseAssignmentRequest(10L, 20L, (short) 7, 70, 50, 2L,
                "Nhan vien tao phieu nhap thu cong", 30L);
    }

    private void authenticate(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null));
    }

    private Employee employee(Long id, RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee employee = new Employee();
        ReflectionTestUtils.setField(employee, "id", id);
        employee.setFullName("User " + id);
        employee.setEmail("user" + id + "@example.com");
        employee.setPasswordHash("secret");
        employee.setRole(role);
        return employee;
    }

    private Product product(Long id) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setCode("SP001");
        product.setName("Laptop");
        return product;
    }

    private Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        ReflectionTestUtils.setField(warehouse, "id", id);
        warehouse.setCode("K001");
        warehouse.setName("Kho chinh");
        return warehouse;
    }

    private ForecastModelMetadata metadata(Long id, Product product, Warehouse warehouse) {
        ForecastModelMetadata metadata = new ForecastModelMetadata();
        ReflectionTestUtils.setField(metadata, "id", id);
        metadata.setProduct(product);
        metadata.setWarehouse(warehouse);
        return metadata;
    }

    private AiPurchaseRequest assignment(Employee receiver) {
        Product product = product(10L);
        Warehouse warehouse = warehouse(20L);
        AiPurchaseRequest assignment = new AiPurchaseRequest();
        ReflectionTestUtils.setField(assignment, "id", 99L);
        assignment.setCode("YCAI-1");
        assignment.setProduct(product);
        assignment.setWarehouse(warehouse);
        assignment.setModelMetadata(metadata(30L, product, warehouse));
        assignment.setHorizonDays((short) 7);
        assignment.setAiSuggestedQuantity(70);
        assignment.setRequestedQuantity(50);
        assignment.setSender(employee(1L, RoleCode.MANAGER));
        assignment.setReceiver(receiver);
        assignment.setContent("Mo StockSense va tao phieu nhap.");
        return assignment;
    }
}
