package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportReceiptServiceImpl implements ExportReceiptService {

    private static final List<ExportReceiptStatus> PENDING_APPROVAL_STATUSES = List.of(
            ExportReceiptStatus.CHO_DUYET_CAP_1,
            ExportReceiptStatus.CHO_DUYET_CAP_2);

    private final ExportReceiptRepository exportReceiptRepository;

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

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        return employee;
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
            throw new BadRequestException("status khong hop le.", exception);
        }
    }
}
