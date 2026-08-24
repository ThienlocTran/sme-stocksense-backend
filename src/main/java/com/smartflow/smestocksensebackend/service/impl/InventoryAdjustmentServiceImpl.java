package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;
import com.smartflow.smestocksensebackend.dto.inventoryadjustment.RejectInventoryAdjustmentRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import com.smartflow.smestocksensebackend.entity.InventoryCount;
import com.smartflow.smestocksensebackend.entity.InventoryCountDetail;
import com.smartflow.smestocksensebackend.entity.InventoryCountStatus;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryAdjustmentRepository;
import com.smartflow.smestocksensebackend.repository.InventoryCountDetailRepository;
import com.smartflow.smestocksensebackend.repository.InventoryCountRepository;
import com.smartflow.smestocksensebackend.service.InventoryAdjustmentCodeGenerator;
import com.smartflow.smestocksensebackend.service.InventoryAdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InventoryAdjustmentServiceImpl implements InventoryAdjustmentService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final InventoryAdjustmentRepository adjustmentRepository;
    private final InventoryCountRepository countRepository;
    private final InventoryCountDetailRepository detailRepository;
    private final InventoryAdjustmentCodeGenerator codeGenerator;

    @Override
    @Transactional
    public InventoryAdjustmentResponse getOrCreateDraft(Long inventoryCountId) {
        return adjustmentRepository.findByInventoryCountId(inventoryCountId)
                .map(this::response)
                .orElseGet(() -> createDraft(inventoryCountId));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAdjustmentResponse getByInventoryCountId(Long inventoryCountId) {
        InventoryAdjustment adjustment = adjustmentRepository.findByInventoryCountId(inventoryCountId)
                .orElseThrow(() -> new NotFoundException("Phieu dieu chinh kiem ke khong ton tai."));
        return response(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryAdjustmentResponse get(Long id) {
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phieu dieu chinh kiem ke khong ton tai."));
        return response(adjustment);
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse submit(Long id) {
        Employee actor = actor();
        ensureOperationalSubmitter(actor);
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phieu dieu chinh kiem ke khong ton tai."));
        ensureSubmitOwner(actor, adjustment);
        if (adjustment.getStatus() != InventoryAdjustmentStatus.NHAP
                && adjustment.getStatus() != InventoryAdjustmentStatus.TU_CHOI) {
            throw new ConflictException("Chi duoc gui duyet phieu dieu chinh o trang thai NHAP hoac TU_CHOI.");
        }

        validateSubmittable(adjustment);

        adjustment.setStatus(InventoryAdjustmentStatus.CHO_DUYET);
        adjustment.setSubmittedBy(actor);
        adjustment.setSubmittedAt(LocalDateTime.now());
        adjustment.setApprovedBy(null);
        adjustment.setApprovedAt(null);
        adjustment.setRejectionReason(null);
        return response(adjustmentRepository.saveAndFlush(adjustment));
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse approve(Long id) {
        Employee actor = actor();
        InventoryAdjustment adjustment = pendingDecision(id, actor);
        adjustment.setStatus(InventoryAdjustmentStatus.DA_DUYET);
        adjustment.setApprovedBy(actor);
        adjustment.setApprovedAt(LocalDateTime.now());
        return response(adjustmentRepository.saveAndFlush(adjustment));
    }

    @Override
    @Transactional
    public InventoryAdjustmentResponse reject(Long id, RejectInventoryAdjustmentRequest request) {
        if (request == null || request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new BadRequestException("Ly do tu choi khong duoc de trong.");
        }
        Employee actor = actor();
        InventoryAdjustment adjustment = pendingDecision(id, actor);
        adjustment.setStatus(InventoryAdjustmentStatus.TU_CHOI);
        adjustment.setApprovedBy(actor);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setRejectionReason(request.rejectionReason().trim());
        return response(adjustmentRepository.saveAndFlush(adjustment));
    }

    private InventoryAdjustmentResponse createDraft(Long inventoryCountId) {
        InventoryCount count = countRepository.findById(inventoryCountId)
                .orElseThrow(() -> new NotFoundException("Dot kiem ke khong ton tai."));
        validateCount(count);
        List<InventoryCountDetail> details = detailRepository.findByInventoryCountIdOrderByIdAsc(inventoryCountId);
        validateDetails(details);

        Employee actor = actor();
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (adjustmentRepository.existsByCodeIgnoreCase(code)) {
                continue;
            }
            InventoryAdjustment adjustment = new InventoryAdjustment();
            adjustment.setCode(code);
            adjustment.setInventoryCount(count);
            adjustment.setStatus(InventoryAdjustmentStatus.NHAP);
            adjustment.setCreatedBy(actor);
            try {
                return response(adjustmentRepository.saveAndFlush(adjustment));
            } catch (DataIntegrityViolationException exception) {
                var existing = adjustmentRepository.findByInventoryCountId(inventoryCountId);
                if (existing.isPresent()) return response(existing.get());
                if (attempt == MAX_CODE_ATTEMPTS - 1) {
                    throw new ConflictException("Khong the tao ma phieu dieu chinh kiem ke duy nhat.");
                }
            }
        }
        throw new ConflictException("Khong the tao ma phieu dieu chinh kiem ke duy nhat.");
    }

    private void validateCount(InventoryCount count) {
        if (count.getStatus() == InventoryCountStatus.DA_CHOT) {
            throw new ConflictException("Dot kiem ke da chot.");
        }
        if (count.getStatus() == InventoryCountStatus.DA_HUY) {
            throw new ConflictException("Dot kiem ke da huy.");
        }
    }

    private void validateDetails(List<InventoryCountDetail> details) {
        if (details.stream().anyMatch(detail -> detail.getActualQuantity() == null)) {
            throw new BadRequestException("Phai nhap so luong thuc te cho tat ca san pham.");
        }
        if (details.stream().noneMatch(detail -> detail.getDifferenceQuantity() != null && detail.getDifferenceQuantity() != 0)) {
            throw new BadRequestException("Dot kiem ke khong co chenh lech.");
        }
    }

    private void validateSubmittable(InventoryAdjustment adjustment) {
        validateCount(adjustment.getInventoryCount());
        List<InventoryCountDetail> details = detailRepository.findByInventoryCountIdOrderByIdAsc(
                adjustment.getInventoryCount().getId()
        );
        validateDetails(details);
        if (details.stream()
                .filter(detail -> detail.getDifferenceQuantity() != null && detail.getDifferenceQuantity() != 0)
                .anyMatch(detail -> detail.getReason() == null || detail.getReason().isBlank())) {
            throw new BadRequestException("Ly do chenh lech la bat buoc.");
        }
    }

    private InventoryAdjustment pendingDecision(Long id, Employee actor) {
        ensureApprovalActor(actor);
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phieu dieu chinh kiem ke khong ton tai."));
        if (adjustment.getStatus() != InventoryAdjustmentStatus.CHO_DUYET) {
            throw new ConflictException("Chi duoc xu ly phieu dieu chinh o trang thai CHO_DUYET.");
        }
        if (adjustment.getSubmittedBy() == null) {
            throw new ConflictException("Phieu dieu chinh chua co nguoi gui duyet.");
        }
        ensureDifferentDecisionActor(actor, adjustment);
        validateSubmittable(adjustment);
        return adjustment;
    }

    private InventoryAdjustmentResponse response(InventoryAdjustment adjustment) {
        List<InventoryCountDetail> details = detailRepository.findByInventoryCountIdOrderByIdAsc(
                adjustment.getInventoryCount().getId()
        );
        return InventoryAdjustmentResponse.from(adjustment, details);
    }

    private void ensureOperationalSubmitter(Employee actor) {
        RoleCode role = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (role != RoleCode.ADMIN && role != RoleCode.EMPLOYEE) {
            throw new AccessDeniedException("Khong co quyen gui duyet phieu dieu chinh kiem ke.");
        }
    }

    private void ensureSubmitOwner(Employee actor, InventoryAdjustment adjustment) {
        RoleCode role = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (role == RoleCode.ADMIN) {
            return;
        }
        if (adjustment.getCreatedBy() == null || !Objects.equals(actor.getId(), adjustment.getCreatedBy().getId())) {
            throw new AccessDeniedException("Chi nguoi tao moi duoc gui duyet phieu dieu chinh kiem ke.");
        }
    }

    private void ensureApprovalActor(Employee actor) {
        RoleCode role = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (role != RoleCode.ADMIN && role != RoleCode.MANAGER) {
            throw new AccessDeniedException("Khong co quyen duyet phieu dieu chinh kiem ke.");
        }
    }

    private void ensureDifferentDecisionActor(Employee actor, InventoryAdjustment adjustment) {
        Long actorId = actor.getId();
        if (adjustment.getCreatedBy() != null && Objects.equals(actorId, adjustment.getCreatedBy().getId())) {
            throw new BadRequestException("Nguoi duyet phai khac nguoi tao phieu.");
        }
        if (Objects.equals(actorId, adjustment.getSubmittedBy().getId())) {
            throw new BadRequestException("Nguoi duyet phai khac nguoi gui duyet.");
        }
    }

    private Employee actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Employee employee) {
            return employee;
        }
        throw new MissingRoleException("Khong xac dinh duoc nguoi dung.");
    }
}
