package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailItemResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ExportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportReceiptServiceImpl implements ExportReceiptService {

    private static final List<ExportReceiptStatus> PENDING_APPROVAL_STATUSES = List.of(
            ExportReceiptStatus.CHO_DUYET_CAP_1,
            ExportReceiptStatus.CHO_DUYET_CAP_2);

    private final ExportReceiptRepository exportReceiptRepository;
    private final ExportReceiptDetailRepository exportReceiptDetailRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryTransactionService inventoryTransactionService;

    @Override
    @Transactional(readOnly = true)
    public ExportReceiptDetailResponse getDetail(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        ExportReceipt receipt = exportReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu xuat khong ton tai."));
        ensureCanReadReceipt(actor, receipt);

        List<ExportReceiptDetail> details = exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(receiptId);
        Map<Long, Integer> inventoryByProductId = loadInventoryByProductId(details, receipt.getWarehouse().getId());
        List<ExportReceiptDetailItemResponse> items = details.stream()
                .map(detail -> {
                    Long productId = detail.getProduct() != null ? detail.getProduct().getId() : null;
                    Integer currentInventory = productId != null ? inventoryByProductId.getOrDefault(productId, 0) : 0;
                    boolean warning = detail.getQuantity() > currentInventory;
                    return ExportReceiptDetailItemResponse.from(detail, currentInventory, warning);
                })
                .toList();

        return ExportReceiptDetailResponse.from(receipt, items);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportReceiptPageResponse listPendingApproval(String status, Pageable pageable) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ExportReceiptStatus parsedStatus = parseStatus(status);
        if (parsedStatus == null) {
            return ExportReceiptPageResponse.from(exportReceiptRepository
                    .findByStatusIn(PENDING_APPROVAL_STATUSES, pageable)
                    .map(ExportReceiptSummaryResponse::from));
        }
        if (!PENDING_APPROVAL_STATUSES.contains(parsedStatus)) {
            throw new BadRequestException(
                    "Chi duoc loc theo trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2).");
        }
        return ExportReceiptPageResponse.from(exportReceiptRepository
                .findByStatus(parsedStatus, pageable)
                .map(ExportReceiptSummaryResponse::from));
    }

    @Override
    @Transactional
    public ExportReceiptDetailResponse approve(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ExportReceipt receipt = exportReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu xuat khong ton tai."));

        LocalDateTime now = LocalDateTime.now();
        try {
            if (receipt.getStatus() == ExportReceiptStatus.CHO_DUYET_CAP_1) {
                receipt.setStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
            } else if (receipt.getStatus() == ExportReceiptStatus.CHO_DUYET_CAP_2) {
                List<ExportReceiptDetail> details = exportReceiptDetailRepository
                        .findByExportReceiptIdOrderByIdAsc(receiptId);
                details = details.stream()
                        .sorted((left, right) -> {
                            Long leftProductId = left.getProduct() != null ? left.getProduct().getId() : null;
                            Long rightProductId = right.getProduct() != null ? right.getProduct().getId() : null;
                            int comparison = Integer.compare(
                                    leftProductId != null ? leftProductId.intValue() : Integer.MAX_VALUE,
                                    rightProductId != null ? rightProductId.intValue() : Integer.MAX_VALUE);
                            if (comparison != 0) {
                                return comparison;
                            }
                            return Long.compare(
                                    left.getId() != null ? left.getId() : Long.MAX_VALUE,
                                    right.getId() != null ? right.getId() : Long.MAX_VALUE);
                        })
                        .toList();
                for (ExportReceiptDetail detail : details) {
                    Long productId = detail.getProduct() != null ? detail.getProduct().getId() : null;
                    Long warehouseId = receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null;
                    if (productId == null || warehouseId == null) {
                        throw new BadRequestException("Chi tiet phieu xuat khong hop le.");
                    }

                    InventoryLevel inventoryLevel = inventoryLevelRepository
                            .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                            .orElseThrow(() -> new ConflictException("Khong du ton kho cho san pham " + productId + "."));

                    int quantityBefore = inventoryLevel.getQuantity();
                    int quantityNeeded = detail.getQuantity() != null ? detail.getQuantity() : 0;
                    if (quantityBefore < quantityNeeded) {
                        throw new ConflictException("Khong du ton kho cho san pham " + productId + ".");
                    }

                    int quantityAfter = quantityBefore - quantityNeeded;
                    inventoryLevel.setQuantity(quantityAfter);
                    inventoryLevelRepository.saveAndFlush(inventoryLevel);
                    inventoryTransactionService.recordTransaction(
                            productId,
                            warehouseId,
                            InventoryTransactionType.XUAT_KHO,
                            quantityNeeded,
                            quantityBefore,
                            quantityAfter,
                            null,
                            "Duyet phieu xuat cap 2");
                }
                receipt.setStatus(ExportReceiptStatus.HOAN_THANH);
            } else {
                throw new ConflictException("Chi duoc duyet phieu xuat o trang thai CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2.");
            }

            if (receipt.getStatus() == ExportReceiptStatus.HOAN_THANH) {
                receipt.setApprovedBy(actor);
                receipt.setApprovedAt(now);
            }
            ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
            List<ExportReceiptDetail> details = exportReceiptDetailRepository
                    .findByExportReceiptIdOrderByIdAsc(receiptId);
            Map<Long, Integer> inventoryByProductId = loadInventoryByProductId(details, savedReceipt.getWarehouse().getId());
            return ExportReceiptDetailResponse.from(savedReceipt, details.stream()
                    .map(detail -> {
                        Long productId = detail.getProduct() != null ? detail.getProduct().getId() : null;
                        Integer currentInventory = productId != null ? inventoryByProductId.getOrDefault(productId, 0) : 0;
                        boolean warning = detail.getQuantity() > currentInventory;
                        return ExportReceiptDetailItemResponse.from(detail, currentInventory, warning);
                    })
                    .toList());
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu xuat da duoc cap nhat boi request khac.", exception);
        }
    }

    private Map<Long, Integer> loadInventoryByProductId(List<ExportReceiptDetail> details, Long warehouseId) {
        List<Long> productIds = details.stream()
                .map(detail -> detail.getProduct() != null ? detail.getProduct().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty() || warehouseId == null) {
            return Map.of();
        }

        return inventoryLevelRepository.findByWarehouseIdAndProductIdIn(warehouseId, productIds).stream()
                .collect(Collectors.toMap(
                        inventory -> inventory.getProduct().getId(),
                        InventoryLevel::getQuantity,
                        (existing, replacement) -> replacement,
                        java.util.LinkedHashMap::new));
    }

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        return employee;
    }

    private void ensureCanReadReceipt(Employee actor, ExportReceipt receipt) {
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode == RoleCode.ADMIN || roleCode == RoleCode.MANAGER) {
            return;
        }
        if (roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException("Khong co quyen xem phieu xuat.");
        }

        Employee creator = receipt.getCreatedBy();
        if (creator == null || !actor.getId().equals(creator.getId())) {
            throw new MissingRoleException("Khong co quyen xem phieu xuat cua nguoi khac.");
        }
    }

    private void ensureCanApprove(Employee actor) {
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.MANAGER) {
            throw new MissingRoleException("Khong co quyen duyet phieu xuat.");
        }
    }

    private ExportReceiptStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ExportReceiptStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("status khong hop le.");
        }
    }
}
