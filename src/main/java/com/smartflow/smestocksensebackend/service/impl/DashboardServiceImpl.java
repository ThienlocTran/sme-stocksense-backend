package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.Employee;

import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;
import com.smartflow.smestocksensebackend.dto.response.OverviewMetricsDTO;
import com.smartflow.smestocksensebackend.dto.response.PendingTasksDTO;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ExportReceiptRepository exportReceiptRepository;
    private final InventoryAlertRepository inventoryAlertRepository;

    private static final List<ImportReceiptStatus> IMPORT_PENDING = List.of(
            ImportReceiptStatus.CHO_DUYET_CAP_1,
            ImportReceiptStatus.CHO_DUYET_CAP_2,
            ImportReceiptStatus.CHO_HANG_VE,
            ImportReceiptStatus.CHO_KIEM_HANG
    );

    private static final List<ExportReceiptStatus> EXPORT_PENDING = List.of(
            ExportReceiptStatus.CHO_DUYET,
            ExportReceiptStatus.DA_DUYET
    );

    private static final List<InventoryAlertStatus> ALERT_PENDING = List.of(
            InventoryAlertStatus.OPEN,
            InventoryAlertStatus.ACKNOWLEDGED
    );

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(Employee principal) {
        if (principal == null || principal.getId() == null) {
            throw new AuthenticationCredentialsNotFoundException("Vui lòng đăng nhập để xem thông tin.");
        }

        Employee employee = employeeRepository.findById(principal.getId())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Tài khoản không tồn tại."));

        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        if (employee.getRole() == null || employee.getRole().getCode() == null) {
            throw new MissingRoleException();
        }

        RoleCode roleCode = employee.getRole().getCode();
        boolean isAdminOrManager = roleCode == RoleCode.ADMIN || roleCode == RoleCode.MANAGER;
        boolean isEmployee = roleCode == RoleCode.EMPLOYEE;

        if (!isAdminOrManager && !isEmployee) {
            throw new AccessDeniedException("Vai trò không hợp lệ để xem Dashboard.");
        }

        // 1. Tải Global Metrics (Cho tất cả Role)
        long totalProducts = productRepository.countByStatus(ProductStatus.HOAT_DONG);
        long totalWarehouses = warehouseRepository.countByStatus(WarehouseStatus.HOAT_DONG);
        long totalStock = inventoryLevelRepository.sumTotalQuantity();

        OverviewMetricsDTO overview = OverviewMetricsDTO.builder()
                .totalProducts(totalProducts)
                .totalWarehouses(totalWarehouses)
                .totalStock(totalStock)
                .build();

        // 2. Tải Pending Tasks theo Role
        long pendingImports;
        long pendingExports;
        long pendingAlerts;

        if (isAdminOrManager) {
            // ADMIN, MANAGER: Đếm toàn hệ thống
            pendingImports = importReceiptRepository.countByStatusIn(IMPORT_PENDING);
            pendingExports = exportReceiptRepository.countByStatusIn(EXPORT_PENDING);
            pendingAlerts = inventoryAlertRepository.countByStatusIn(ALERT_PENDING);
        } else {
            // EMPLOYEE: Chỉ đếm phiếu của mình tạo, Alert thì đếm Global (vì Alert không có createdBy)
            pendingImports = importReceiptRepository.countByStatusInAndCreatedById(IMPORT_PENDING, employee.getId());
            pendingExports = exportReceiptRepository.countByStatusInAndCreatedById(EXPORT_PENDING, employee.getId());
            pendingAlerts = inventoryAlertRepository.countByStatusIn(ALERT_PENDING);
        }

        PendingTasksDTO pendingTasks = PendingTasksDTO.builder()
                .importReceipts(pendingImports)
                .exportReceipts(pendingExports)
                .inventoryAlerts(pendingAlerts)
                .build();

        return DashboardOverviewResponse.builder()
                .overview(overview)
                .pendingTasks(pendingTasks)
                .build();
    }
}
