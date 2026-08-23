package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import com.smartflow.smestocksensebackend.entity.InventoryCount;
import com.smartflow.smestocksensebackend.entity.InventoryCountDetail;
import com.smartflow.smestocksensebackend.entity.InventoryCountStatus;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private InventoryAdjustmentResponse response(InventoryAdjustment adjustment) {
        List<InventoryCountDetail> details = detailRepository.findByInventoryCountIdOrderByIdAsc(
                adjustment.getInventoryCount().getId()
        );
        return InventoryAdjustmentResponse.from(adjustment, details);
    }

    private Employee actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Employee employee) {
            return employee;
        }
        throw new MissingRoleException("Khong xac dinh duoc nguoi dung.");
    }
}
