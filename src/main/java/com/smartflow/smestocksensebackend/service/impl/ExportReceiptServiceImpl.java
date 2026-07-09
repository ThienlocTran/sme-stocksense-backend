package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.outbound.ExportReceiptStatePolicy;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailItemResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDetailRequest;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptResponse;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.*;
import com.smartflow.smestocksensebackend.service.ExportReceiptCodeGenerator;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportReceiptServiceImpl implements ExportReceiptService {

    private static final List<ExportReceiptStatus> PENDING_APPROVAL_STATUSES = List.of(
            ExportReceiptStatus.CHO_DUYET_CAP_1,
            ExportReceiptStatus.CHO_DUYET_CAP_2);

    private static final int MAX_CODE_ATTEMPTS = 3;

    private final ExportReceiptRepository exportReceiptRepository;
    private final ExportReceiptDetailRepository exportReceiptDetailRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final ExportReceiptCodeGenerator codeGenerator;
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

    @Override
    @Transactional
    public ExportReceiptResponse createDraft(ExportReceiptDraftRequest request) {
        // 1. Xác thực người dùng
        Employee creator = currentEmployee();
        if (creator.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        // 2. Validate Kho hàng
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());

        // 3. Validate Đối tác (nếu có)
        Partner partner = validatePartner(request.getPartnerId());

        // 4. Validate và tính toán danh sách sản phẩm
        List<ExportReceiptDetail> details = buildDetailsAndValidateInventory(request.getDetails(), warehouse.getId());
        BigDecimal totalAmount = details.stream()
                .map(ExportReceiptDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Sinh mã và Lưu DB
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (exportReceiptRepository.existsByCodeIgnoreCase(code)) {
                continue;
            }

            ExportReceipt receipt = new ExportReceipt();
            receipt.setCode(code);
            receipt.setWarehouse(warehouse);
            receipt.setPartner(partner);
            receipt.setCreatedBy(creator);
            receipt.setStatus(ExportReceiptStatus.NHAP);
            receipt.setTotalAmount(totalAmount);
            receipt.setNote(request.getNote());

            try {
                ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
                for (ExportReceiptDetail detail : details) {
                    detail.setExportReceipt(savedReceipt);
                }
                exportReceiptDetailRepository.saveAllAndFlush(details);
                return ExportReceiptResponse.from(savedReceipt, details);
            } catch (DataIntegrityViolationException exception) {
                if (attempt == MAX_CODE_ATTEMPTS - 1) {
                    throw new ConflictException("Không thể tạo mã phiếu xuất duy nhất.");
                }
            }
        }

        throw new ConflictException("Không thể tạo mã phiếu xuất duy nhất.");
    }

    @Override
    @Transactional
    public ExportReceiptResponse updateDraft(Long id, ExportReceiptDraftRequest request) {
        // 1. Lấy user hiện tại
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        // 2. Tìm phiếu xuất
        ExportReceipt receipt = exportReceiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu xuất không tồn tại."));

        // 3. Phân quyền: Chỉ người tạo mới được sửa
        if (!actor.getId().equals(receipt.getCreatedBy().getId()) && actor.getRole().getCode() != RoleCode.ADMIN) {
            throw new MissingRoleException("Bạn không có quyền sửa phiếu xuất này.");
        }

        // 4. Kiểm tra trạng thái: Chỉ sửa khi ở trạng thái NHAP hoặc TU_CHOI
        if (!ExportReceiptStatePolicy.isEditable(receipt.getStatus())) {
            throw new ConflictException("Chỉ được sửa phiếu xuất ở trạng thái NHÁP hoặc TỪ CHỐI.");
        }

        // 5. Validate dữ liệu đầu vào
        Warehouse warehouse = validateWarehouse(request.getWarehouseId());
        Partner partner = validatePartner(request.getPartnerId());
        List<ExportReceiptDetail> newDetails = buildDetailsAndValidateInventory(request.getDetails(), warehouse.getId());
        BigDecimal totalAmount = newDetails.stream()
                .map(ExportReceiptDetail::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Cập nhật phiếu master
        receipt.setWarehouse(warehouse);
        receipt.setPartner(partner);
        receipt.setTotalAmount(totalAmount);
        receipt.setNote(request.getNote());
        ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);

        // 7. Xóa details cũ và lưu details mới (Replace All)
        exportReceiptDetailRepository.deleteByExportReceiptId(id);
        for (ExportReceiptDetail detail : newDetails) {
            detail.setExportReceipt(savedReceipt);
        }
        exportReceiptDetailRepository.saveAllAndFlush(newDetails);

        return ExportReceiptResponse.from(savedReceipt, newDetails);
    }

    // --- Helper Methods ---

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        return employee;
    }

    private Warehouse validateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) {
            throw new BadRequestException("Kho hàng không hoạt động.");
        }
        return warehouse;
    }

    private Partner validatePartner(Long partnerId) {
        if (partnerId == null) return null;
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại."));
        if (partner.getStatus() != PartnerStatus.HOAT_DONG) {
            throw new BadRequestException("Khách hàng không hoạt động.");
        }
        return partner;
    }

    private List<ExportReceiptDetail> buildDetailsAndValidateInventory(List<ExportReceiptDetailRequest> detailRequests, Long warehouseId) {
        if (detailRequests == null || detailRequests.isEmpty()) {
            throw new BadRequestException("Danh sách sản phẩm không được rỗng.");
        }

        Set<Long> productIds = new HashSet<>();
        List<ExportReceiptDetail> details = new ArrayList<>();

        for (ExportReceiptDetailRequest req : detailRequests) {
            // Kiểm tra sản phẩm trùng lặp
            if (!productIds.add(req.getProductId())) {
                throw new BadRequestException("Sản phẩm trùng lặp trong danh sách. Vui lòng gộp số lượng.");
            }

            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại: " + req.getProductId()));

            // Kiểm tra tồn kho nghiêm ngặt
            InventoryLevel inventory = inventoryLevelRepository.findByProductIdAndWarehouseId(product.getId(), warehouseId)
                    .orElseThrow(() -> new BadRequestException("Sản phẩm " + product.getName() + " không có trong kho này."));

            if (req.getQuantity() > inventory.getQuantity()) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " chỉ còn " + inventory.getQuantity() + " trong kho, không đủ xuất " + req.getQuantity() + ".");
            }

            ExportReceiptDetail detail = new ExportReceiptDetail();
            detail.setProduct(product);
            detail.setQuantity(req.getQuantity());
            
            BigDecimal unitPrice = req.getUnitPrice() != null ? req.getUnitPrice() : BigDecimal.ZERO;
            detail.setUnitPrice(unitPrice);
            detail.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(req.getQuantity())));
            detail.setNote(req.getNote());
            
            details.add(detail);
        }
        return details;
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
            throw new BadRequestException("status khong hop le.", exception);
        }
    }
}
