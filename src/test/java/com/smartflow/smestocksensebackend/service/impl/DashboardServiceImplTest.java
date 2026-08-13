package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private ImportReceiptRepository importReceiptRepository;

    @Mock
    private ExportReceiptRepository exportReceiptRepository;

    @Mock
    private InventoryAlertRepository inventoryAlertRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Employee adminPrincipal;
    private Employee managerPrincipal;
    private Employee employeePrincipal;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role();
        adminRole.setCode(RoleCode.ADMIN);
        adminPrincipal = new Employee();
        adminPrincipal.setId(1L);
        adminPrincipal.setRole(adminRole);
        adminPrincipal.setStatus(EmployeeStatus.HOAT_DONG);

        Role managerRole = new Role();
        managerRole.setCode(RoleCode.MANAGER);
        managerPrincipal = new Employee();
        managerPrincipal.setId(2L);
        managerPrincipal.setRole(managerRole);
        managerPrincipal.setStatus(EmployeeStatus.HOAT_DONG);

        Role employeeRole = new Role();
        employeeRole.setCode(RoleCode.EMPLOYEE);
        employeePrincipal = new Employee();
        employeePrincipal.setId(3L);
        employeePrincipal.setRole(employeeRole);
        employeePrincipal.setStatus(EmployeeStatus.HOAT_DONG);

        lenient().when(employeeRepository.findById(1L)).thenReturn(Optional.of(adminPrincipal));
        lenient().when(employeeRepository.findById(2L)).thenReturn(Optional.of(managerPrincipal));
        lenient().when(employeeRepository.findById(3L)).thenReturn(Optional.of(employeePrincipal));
    }

    @Test
    void getOverview_Admin_ReturnsGlobalMetrics() {
        // Arrange
        when(productRepository.countByStatus(ProductStatus.HOAT_DONG)).thenReturn(100L);
        when(warehouseRepository.countByStatus(WarehouseStatus.HOAT_DONG)).thenReturn(5L);
        when(inventoryLevelRepository.sumTotalQuantity()).thenReturn(5000L);

        when(importReceiptRepository.countByStatusIn(any())).thenReturn(10L);
        when(exportReceiptRepository.countByStatusIn(any())).thenReturn(5L);
        when(inventoryAlertRepository.countByStatusIn(any())).thenReturn(3L);

        // Act
        DashboardOverviewResponse response = dashboardService.getOverview(adminPrincipal);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getOverview().getTotalProducts());
        assertEquals(5L, response.getOverview().getTotalWarehouses());
        assertEquals(5000L, response.getOverview().getTotalStock());
        
        assertEquals(10L, response.getPendingTasks().getImportReceipts());
        assertEquals(5L, response.getPendingTasks().getExportReceipts());
        assertEquals(3L, response.getPendingTasks().getInventoryAlerts());

        // Verify correct repository methods are called for Admin
        org.mockito.Mockito.verify(importReceiptRepository).countByStatusIn(any());
        org.mockito.Mockito.verify(exportReceiptRepository).countByStatusIn(any());
        org.mockito.Mockito.verify(importReceiptRepository, org.mockito.Mockito.never()).countByStatusInAndCreatedById(any(), any());
        org.mockito.Mockito.verify(exportReceiptRepository, org.mockito.Mockito.never()).countByStatusInAndCreatedById(any(), any());
    }

    @Test
    void getOverview_ExportPendingStatuses_MatchDbEnum() {
        when(productRepository.countByStatus(ProductStatus.HOAT_DONG)).thenReturn(0L);
        when(warehouseRepository.countByStatus(WarehouseStatus.HOAT_DONG)).thenReturn(0L);
        when(inventoryLevelRepository.sumTotalQuantity()).thenReturn(0L);
        when(importReceiptRepository.countByStatusIn(any())).thenReturn(0L);
        when(exportReceiptRepository.countByStatusIn(any())).thenReturn(0L);
        when(inventoryAlertRepository.countByStatusIn(any())).thenReturn(0L);

        dashboardService.getOverview(adminPrincipal);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExportReceiptStatus>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(exportReceiptRepository).countByStatusIn(captor.capture());
        assertEquals(List.of(
                ExportReceiptStatus.CHO_DUYET_CAP_1,
                ExportReceiptStatus.CHO_DUYET_CAP_2,
                ExportReceiptStatus.DA_DUYET
        ), captor.getValue());
    }

    @Test
    void getOverview_Manager_ReturnsGlobalMetrics() {
        // Arrange
        when(productRepository.countByStatus(ProductStatus.HOAT_DONG)).thenReturn(100L);
        when(warehouseRepository.countByStatus(WarehouseStatus.HOAT_DONG)).thenReturn(5L);
        when(inventoryLevelRepository.sumTotalQuantity()).thenReturn(5000L);

        when(importReceiptRepository.countByStatusIn(any())).thenReturn(10L);
        when(exportReceiptRepository.countByStatusIn(any())).thenReturn(5L);
        when(inventoryAlertRepository.countByStatusIn(any())).thenReturn(3L);

        // Act
        DashboardOverviewResponse response = dashboardService.getOverview(managerPrincipal);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.getPendingTasks().getImportReceipts());
        assertEquals(5L, response.getPendingTasks().getExportReceipts());
        assertEquals(3L, response.getPendingTasks().getInventoryAlerts());

        // Verify correct repository methods are called for Manager
        org.mockito.Mockito.verify(importReceiptRepository).countByStatusIn(any());
        org.mockito.Mockito.verify(exportReceiptRepository).countByStatusIn(any());
        org.mockito.Mockito.verify(importReceiptRepository, org.mockito.Mockito.never()).countByStatusInAndCreatedById(any(), any());
        org.mockito.Mockito.verify(exportReceiptRepository, org.mockito.Mockito.never()).countByStatusInAndCreatedById(any(), any());
    }

    @Test
    void getOverview_Employee_ReturnsFilteredMetrics() {
        // Arrange
        when(productRepository.countByStatus(ProductStatus.HOAT_DONG)).thenReturn(100L);
        when(warehouseRepository.countByStatus(WarehouseStatus.HOAT_DONG)).thenReturn(5L);
        when(inventoryLevelRepository.sumTotalQuantity()).thenReturn(5000L);

        // Filtered by createdById
        when(importReceiptRepository.countByStatusInAndCreatedById(any(), eq(3L))).thenReturn(2L);
        when(exportReceiptRepository.countByStatusInAndCreatedById(any(), eq(3L))).thenReturn(1L);
        // Alert is still global
        when(inventoryAlertRepository.countByStatusIn(any())).thenReturn(3L);

        // Act
        DashboardOverviewResponse response = dashboardService.getOverview(employeePrincipal);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getOverview().getTotalProducts());
        assertEquals(2L, response.getPendingTasks().getImportReceipts());
        assertEquals(1L, response.getPendingTasks().getExportReceipts());
        assertEquals(3L, response.getPendingTasks().getInventoryAlerts());

        // Verify correct repository methods are called for Employee
        org.mockito.Mockito.verify(importReceiptRepository).countByStatusInAndCreatedById(any(), org.mockito.ArgumentMatchers.eq(3L));
        org.mockito.Mockito.verify(exportReceiptRepository).countByStatusInAndCreatedById(any(), org.mockito.ArgumentMatchers.eq(3L));
        org.mockito.Mockito.verify(importReceiptRepository, org.mockito.Mockito.never()).countByStatusIn(any());
        org.mockito.Mockito.verify(exportReceiptRepository, org.mockito.Mockito.never()).countByStatusIn(any());
    }

    @Test
    void getOverview_EmptyInventory_ReturnsZero() {
        // Arrange
        when(productRepository.countByStatus(ProductStatus.HOAT_DONG)).thenReturn(0L);
        when(warehouseRepository.countByStatus(WarehouseStatus.HOAT_DONG)).thenReturn(0L);
        when(inventoryLevelRepository.sumTotalQuantity()).thenReturn(0L); // simulate COALESCE returning 0

        // Act
        DashboardOverviewResponse response = dashboardService.getOverview(adminPrincipal);

        // Assert
        assertEquals(0L, response.getOverview().getTotalStock());
        assertEquals(0L, response.getOverview().getTotalProducts());
        assertEquals(0L, response.getOverview().getTotalWarehouses());
    }

    @Test
    void getOverview_NoPendingTasks_ReturnsZero() {
        // Arrange
        when(importReceiptRepository.countByStatusIn(any())).thenReturn(0L);
        when(exportReceiptRepository.countByStatusIn(any())).thenReturn(0L);
        when(inventoryAlertRepository.countByStatusIn(any())).thenReturn(0L);

        // Act
        DashboardOverviewResponse response = dashboardService.getOverview(adminPrincipal);

        // Assert
        assertEquals(0L, response.getPendingTasks().getImportReceipts());
        assertEquals(0L, response.getPendingTasks().getExportReceipts());
        assertEquals(0L, response.getPendingTasks().getInventoryAlerts());
    }

    @Test
    void getOverview_WhenRoleCodeIsNull_ShouldThrowAccessDenied() {
        // Arrange
        Role testRole = new Role();
        testRole.setId(4L);
        // leaving code as null or simulating another value isn't directly possible with Enum if it's strictly ADMIN/MANAGER/EMPLOYEE, but we can set code to null to trigger first if.
        // Actually, what if we have another role? Let's just say employee role is null.
        Employee unknownPrincipal = new Employee();
        unknownPrincipal.setId(4L);
        unknownPrincipal.setRole(testRole);
        unknownPrincipal.setStatus(EmployeeStatus.HOAT_DONG);
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(unknownPrincipal));

        // Act & Assert
        MissingRoleException exception = assertThrows(MissingRoleException.class,
                () -> dashboardService.getOverview(unknownPrincipal));
        assertEquals("Tài khoản chưa được gán vai trò.", exception.getMessage());
    }

    @Test
    void getOverview_NullPrincipal_ThrowsAccessDeniedException() {
        // Act & Assert
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> dashboardService.getOverview(null));
    }
}
