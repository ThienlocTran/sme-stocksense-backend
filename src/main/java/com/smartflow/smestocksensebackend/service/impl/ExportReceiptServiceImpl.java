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

@Service
@RequiredArgsConstructor
public class ExportReceiptServiceImpl implements ExportReceiptService {

    private static final List<ExportReceiptStatus> PENDING_APPROVAL_STATUSES = List.of(
            ExportReceiptStatus.CHO_DUYET_CAP_1,
            ExportReceiptStatus.CHO_DUYET_CAP_2);

    private final ExportReceiptRepository exportReceiptRepository;
    private final ExportReceiptDetailRepository exportReceiptDetailRepository;
    private final InventoryLevelRepository inventoryLevelRepository;

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
        List<ExportReceiptDetailItemResponse> items = details.stream()
                .map(detail -> {
                    Integer currentInventory = inventoryLevelRepository
                            .findByProductIdAndWarehouseId(detail.getProduct().getId(), receipt.getWarehouse().getId())
                            .map(InventoryLevel::getQuantity)
                            .orElse(0);
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

        if (receipt.getStatus() != ExportReceiptStatus.CHO_DUYET_CAP_1) {
            throw new ConflictException("Chi duoc duyet phieu xuat o trang thai CHO_DUYET_CAP_1.");
        }

        LocalDateTime now = LocalDateTime.now();
        receipt.setStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
        receipt.setApprovedBy(actor);
        receipt.setApprovedAt(now);

        try {
            ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
            List<ExportReceiptDetail> details = exportReceiptDetailRepository
                    .findByExportReceiptIdOrderByIdAsc(receiptId);
            return ExportReceiptDetailResponse.from(savedReceipt, details.stream()
                    .map(detail -> {
                        Integer currentInventory = inventoryLevelRepository
                                .findByProductIdAndWarehouseId(detail.getProduct().getId(), savedReceipt.getWarehouse().getId())
                                .map(InventoryLevel::getQuantity)
                                .orElse(0);
                        boolean warning = detail.getQuantity() > currentInventory;
                        return ExportReceiptDetailItemResponse.from(detail, currentInventory, warning);
                    })
                    .toList());
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu xuat da duoc cap nhat boi request khac.");
        }
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
