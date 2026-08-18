package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.dashboard.InventoryMovementProjection;
import com.smartflow.smestocksensebackend.dto.dashboard.StockHealthProjection;
import com.smartflow.smestocksensebackend.entity.Employee;

import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;
import com.smartflow.smestocksensebackend.dto.response.InventoryMovementPointResponse;
import com.smartflow.smestocksensebackend.dto.response.OverviewMetricsDTO;
import com.smartflow.smestocksensebackend.dto.response.PendingTasksDTO;
import com.smartflow.smestocksensebackend.dto.response.StockHealthResponse;
import com.smartflow.smestocksensebackend.dto.response.WarehouseDistributionResponse;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

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
    private final InventoryTransactionRepository inventoryTransactionRepository;

    private static final long MAX_MOVEMENT_DAYS = 366;

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
        Employee employee = requireDashboardAccess(principal);

        RoleCode roleCode = employee.getRole().getCode();
        boolean isAdminOrManager = roleCode == RoleCode.ADMIN || roleCode == RoleCode.MANAGER;
        boolean isEmployee = roleCode == RoleCode.EMPLOYEE;

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

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementPointResponse> getInventoryMovement(Employee principal, LocalDate from, LocalDate to,
            Long warehouseId) {
        requireDashboardAccess(principal);
        validateMovementRange(from, to);

        Map<LocalDate, InventoryMovementProjection> byDate = inventoryTransactionRepository
                .sumDashboardMovement(from, to, warehouseId)
                .stream()
                .collect(Collectors.toMap(InventoryMovementProjection::getDate, projection -> projection));

        return from.datesUntil(to.plusDays(1))
                .map(date -> {
                    InventoryMovementProjection projection = byDate.get(date);
                    return new InventoryMovementPointResponse(
                            date,
                            projection == null ? 0L : projection.getInboundQuantity(),
                            projection == null ? 0L : projection.getOutboundQuantity());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StockHealthResponse getStockHealth(Employee principal) {
        requireDashboardAccess(principal);
        StockHealthProjection counts = inventoryLevelRepository.countDashboardStockHealth();
        return new StockHealthResponse(counts.getHealthy(), counts.getLowStock(), counts.getOutOfStock());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDistributionResponse> getWarehouseDistribution(Employee principal) {
        requireDashboardAccess(principal);
        return inventoryLevelRepository.sumDashboardWarehouseDistribution()
                .stream()
                .map(row -> new WarehouseDistributionResponse(row.getWarehouseId(), row.getWarehouseName(),
                        row.getTotalQuantity()))
                .toList();
    }

    private Employee requireDashboardAccess(Employee principal) {
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

        return employee;
    }

    private void validateMovementRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("from và to không được để trống.");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from phải nhỏ hơn hoặc bằng to.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_MOVEMENT_DAYS) {
            throw new BadRequestException("Khoảng thời gian không được vượt quá 366 ngày.");
        }
    }
}
