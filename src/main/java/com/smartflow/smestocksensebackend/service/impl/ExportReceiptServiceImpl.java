package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.outbound.ExportReceiptStatePolicy;
import com.smartflow.smestocksensebackend.dto.inbound.RejectExportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailItemResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptHistoryResponse;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDetailRequest;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptSubmitRequest;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ExportReceiptHistoryRepository exportReceiptHistoryRepository;

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

        return buildDetailResponse(receipt);
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
    public ExportReceiptDetailResponse reject(Long receiptId, RejectExportReceiptRequest request) {
        if (request == null || request.rejectReason() == null) {
            throw new BadRequestException("Ly do tu choi khong duoc de trong.");
        }

        String trimmedReason = request.rejectReason().trim();
        if (trimmedReason.isBlank()) {
            throw new BadRequestException("Ly do tu choi khong duoc de trong.");
        }
        if (trimmedReason.length() > 500) {
            throw new BadRequestException("Ly do tu choi khong duoc vuot qua 500 ky tu.");
        }

        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ExportReceipt receipt = exportReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu xuat khong ton tai."));

        ExportReceiptStatus current = receipt.getStatus();
        if (!PENDING_APPROVAL_STATUSES.contains(current)) {
            throw new ConflictException("Chi duoc tu choi phieu xuat o trang thai cho duyet.");
        }

        try {
            receipt.setStatus(ExportReceiptStatus.TU_CHOI);
            receipt.setRejectionReason(trimmedReason);
            receipt.setRejectedBy(actor);
            receipt.setRejectedAt(LocalDateTime.now());
            ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
            saveHistory(savedReceipt, actor, ExportReceiptAction.TU_CHOI, trimmedReason);
            return buildDetailResponse(savedReceipt);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu xuat da duoc cap nhat boi request khac.", exception);
        }
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

        // M6 fix: chặn tự duyệt phiếu xuất
        if (actor.getId().equals(receipt.getCreatedBy().getId())) {
            throw new BadRequestException("Nguoi tao phieu khong duoc tu duyet phieu xuat cua chinh minh.");
        }
        if (receipt.getSubmittedBy() != null && actor.getId().equals(receipt.getSubmittedBy().getId())) {
            throw new BadRequestException("Nguoi gui duyet khong duoc tu duyet phieu xuat cua chinh minh.");
        }


        try {
            LocalDateTime now = LocalDateTime.now();
            ExportReceiptAction historyAction;
            if (receipt.getStatus() == ExportReceiptStatus.CHO_DUYET_CAP_1
                    || receipt.getStatus() == ExportReceiptStatus.CHO_DUYET_CAP_2) {
                List<ExportReceiptDetail> details = exportReceiptDetailRepository
                        .findByExportReceiptIdOrderByIdAsc(receiptId);
                details = details.stream()
                        .sorted((left, right) -> {
                            Long leftProductId = left.getProduct() != null ? left.getProduct().getId() : null;
                            Long rightProductId = right.getProduct() != null ? right.getProduct().getId() : null;
                            int comparison = Long.compare(
                                    leftProductId != null ? leftProductId : Long.MAX_VALUE,
                                    rightProductId != null ? rightProductId : Long.MAX_VALUE);
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
                            .orElseThrow(
                                    () -> new ConflictException("Khong du ton kho cho san pham " + productId + "."));

                    int quantityBefore = inventoryLevel.getQuantity();
                    int quantityNeeded = detail.getQuantity() != null ? detail.getQuantity() : 0;
                    if (quantityBefore < quantityNeeded) {
                        throw new ConflictException("Khong du ton kho cho san pham " + productId + ".");
                    }

                    int quantityAfter = quantityBefore - quantityNeeded;
                    inventoryLevel.setQuantity(quantityAfter);
                    inventoryLevelRepository.saveAndFlush(inventoryLevel);
                    inventoryTransactionService.recordExportTransaction(
                            productId,
                            warehouseId,
                            InventoryTransactionType.XUAT_KHO,
                            quantityNeeded,
                            quantityBefore,
                            quantityAfter,
                            receipt,
                            "Duyet phieu xuat cap 2");
                }
                receipt.setStatus(ExportReceiptStatus.HOAN_THANH);
                historyAction = ExportReceiptAction.DUYET_CAP_2;
            } else {
                throw new ConflictException(
                        "Chi duoc duyet phieu xuat o trang thai CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2.");
            }

            if (receipt.getStatus() == ExportReceiptStatus.HOAN_THANH) {
                receipt.setApprovedBy(actor);
                receipt.setApprovedAt(now);
            }
            ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
            saveHistory(savedReceipt, actor, historyAction, null);
            return buildDetailResponse(savedReceipt);
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

            ExportReceipt savedReceipt;
            try {
                savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
            } catch (DataIntegrityViolationException exception) {
                if (attempt == MAX_CODE_ATTEMPTS - 1) {
                    throw new ConflictException("Không thể tạo mã phiếu xuất duy nhất.");
                }
                continue;
            }

            for (ExportReceiptDetail detail : details) {
                detail.setExportReceipt(savedReceipt);
            }
            exportReceiptDetailRepository.saveAllAndFlush(details);
            return ExportReceiptResponse.from(savedReceipt, details);
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
        List<ExportReceiptDetail> newDetails = buildDetailsAndValidateInventory(request.getDetails(),
                warehouse.getId());
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

    @Override
    @Transactional
    public void cancelDraft(Long id) {
        // 1. Lấy thông tin người dùng đang thực hiện request
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        // 2. Tìm phiếu xuất kho trong Database
        ExportReceipt receipt = exportReceiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu xuất không tồn tại."));

        // 3. Phân quyền (Authorization): Chỉ người tạo phiếu HOẶC Admin mới được quyền
        // Hủy
        if (!actor.getId().equals(receipt.getCreatedBy().getId()) && actor.getRole().getCode() != RoleCode.ADMIN) {
            throw new MissingRoleException("Bạn không có quyền hủy phiếu xuất này.");
        }

        // 4. Kiểm tra vòng đời trạng thái (State Policy): Chỉ Hủy khi phiếu đang NHÁP
        // hoặc TỪ CHỐI
        if (!ExportReceiptStatePolicy.isEditable(receipt.getStatus())) {
            throw new ConflictException("Chỉ được hủy phiếu xuất ở trạng thái NHÁP hoặc TỪ CHỐI.");
        }

        // 5. Cập nhật trạng thái thành ĐÃ HỦY (Soft Delete) thay vì xóa vật lý khỏi DB
        receipt.setStatus(ExportReceiptStatus.HUY);
        ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
        saveHistory(savedReceipt, actor, ExportReceiptAction.HUY, null);
    }

    @Override
    @Transactional
    public ExportReceiptResponse submitForApproval(Long id, ExportReceiptSubmitRequest request) {
        // 1. Xác thực người dùng đang thực hiện
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        // 2. Tìm phiếu xuất
        ExportReceipt receipt = exportReceiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu xuất không tồn tại."));

        // 3. Phân quyền: Chỉ người tạo phiếu HOẶC Admin mới được quyền Gửi duyệt
        if (!actor.getId().equals(receipt.getCreatedBy().getId()) && actor.getRole().getCode() != RoleCode.ADMIN) {
            throw new MissingRoleException("Bạn không có quyền gửi duyệt phiếu xuất này.");
        }

        // 4. Kiểm tra trạng thái: Chỉ Gửi duyệt khi phiếu đang NHÁP hoặc TỪ CHỐI
        if (!ExportReceiptStatePolicy.isEditable(receipt.getStatus())) {
            throw new ConflictException("Chỉ được gửi duyệt phiếu xuất ở trạng thái NHÁP hoặc TỪ CHỐI.");
        }

        // 5. Optimistic Locking: Kiểm tra version để tránh xung đột đồng thời
        if (!receipt.getVersion().equals(request.getVersion())) {
            throw new ConflictException("Phiếu xuất đã được cập nhật bởi người khác. Vui lòng tải lại trang.");
        }

        // 6. Kiểm tra lại toàn bộ Tồn kho (Strict Inventory Check) tại chính thời điểm Submit
        List<ExportReceiptDetail> details = exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(id);
        if (details.isEmpty()) {
            throw new BadRequestException("Phiếu xuất chưa có sản phẩm nào.");
        }

        for (ExportReceiptDetail detail : details) {
            Product product = detail.getProduct();
            Warehouse warehouse = receipt.getWarehouse();

            InventoryLevel inventory = inventoryLevelRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Sản phẩm " + product.getName() + " không có trong kho " + warehouse.getName() + "."));

            if (detail.getQuantity() > inventory.getQuantity()) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " chỉ còn " + inventory.getQuantity()
                        + " trong kho, không đủ xuất " + detail.getQuantity() + ". Vui lòng sửa lại phiếu nháp.");
            }
        }

        // 7. Người tạo gửi phiếu đồng nghĩa đã duyệt cấp 1, chuyển thẳng sang chờ cấp 2.
        LocalDateTime submittedAt = LocalDateTime.now();
        receipt.setStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
        receipt.setSubmittedBy(actor);
        receipt.setSubmittedAt(submittedAt);
        receipt.setLevel1ApprovedBy(actor);
        receipt.setLevel1ApprovedAt(submittedAt);

        // Lưu ý: Trường version sẽ được Hibernate tự động tăng lên 1 nhờ @Version trên
        // Entity
        ExportReceipt savedReceipt = exportReceiptRepository.saveAndFlush(receipt);
        saveHistory(savedReceipt, actor, ExportReceiptAction.GUI_DUYET, null);

        return ExportReceiptResponse.from(savedReceipt, details);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listReceipts(
            String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code, org.springframework.data.domain.Pageable pageable) {
        
        // 1. Khởi tạo Specification cho việc lọc động nhiều tiêu chí
        org.springframework.data.jpa.domain.Specification<ExportReceipt> spec = buildSearchSpec(null, status, fromDate, toDate, warehouseId, code);
        
        // 2. Query từ CSDL kết hợp phân trang
        // ponytail: Trực tiếp map sang DTO thông qua hàm record, bỏ qua wrapper
        return exportReceiptRepository.findAll(spec, pageable)
                .map(com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listMyReceipts(
            String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code, org.springframework.data.domain.Pageable pageable) {
        
        // 1. Xác thực user đang gọi API để ép tham số `employeeId`
        Employee actor = currentEmployee();
        
        // 2. Khởi tạo Specification lọc phiếu (trong đó bắt buộc phải có điều kiện `employeeId`)
        org.springframework.data.jpa.domain.Specification<ExportReceipt> spec = buildSearchSpec(actor.getId(), status, fromDate, toDate, warehouseId, code);
        
        // 3. Truy vấn và map sang dạng DTO rút gọn
        return exportReceiptRepository.findAll(spec, pageable)
                .map(com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse::from);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ExportReceiptResponse getReceiptDetails(Long id) {
        // 1. Lấy thông tin user hiện tại
        Employee actor = currentEmployee();

        // 2. Tìm phiếu xuất trong CSDL (Không filter status HUY để hỗ trợ truy vấn lại lịch sử)
        // ponytail: Dùng thẳng findById của JpaRepository, tái sử dụng method có sẵn.
        ExportReceipt receipt = exportReceiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu xuất không tồn tại."));

        // 3. Phân quyền: Chỉ tác giả phiếu HOẶC Admin mới được phép xem chi tiết
        if (!actor.getId().equals(receipt.getCreatedBy().getId()) && actor.getRole().getCode() != RoleCode.ADMIN) {
            throw new MissingRoleException("Bạn không có quyền xem chi tiết phiếu xuất này.");
        }

        // 4. Lấy danh sách sản phẩm chi tiết của phiếu xuất
        List<ExportReceiptDetail> details = exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(id);

        // 5. Build và trả về Response DTO tái sử dụng
        return ExportReceiptResponse.from(receipt, details);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportReceiptHistoryResponse> getHistory(Long id) {
        Employee actor = currentEmployee();
        ExportReceipt receipt = exportReceiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu xuất không tồn tại."));
        ensureCanReadReceipt(actor, receipt);
        if (exportReceiptHistoryRepository == null) {
            return List.of();
        }
        return exportReceiptHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(id).stream()
                .map(ExportReceiptHistoryResponse::from)
                .toList();
    }

    private void saveHistory(ExportReceipt receipt, Employee actor, ExportReceiptAction action, String note) {
        if (exportReceiptHistoryRepository == null) {
            return;
        }
        ExportReceiptHistory history = new ExportReceiptHistory();
        history.setDocument(receipt);
        history.setActor(actor);
        history.setAction(action);
        history.setNote(note);
        exportReceiptHistoryRepository.save(history);
    }

    // --- Helper Methods ---

    // ponytail: Tối giản hóa logic build JPA Specification, không đẻ thêm file Specification/Criteria cồng kềnh.
    private org.springframework.data.jpa.domain.Specification<ExportReceipt> buildSearchSpec(
            Long employeeId, String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            // Lọc theo người tạo phiếu (Dùng cho API listMyReceipts)
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), employeeId));
            }
            if (status != null && !status.isBlank()) {
                try {
                    ExportReceiptStatus parsedStatus = ExportReceiptStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), parsedStatus));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Trạng thái không hợp lệ: " + status);
                }
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouse").get("id"), warehouseId));
            }
            if (code != null && !code.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                // To the end of the day
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59, 999999999)));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ExportReceiptDetailResponse buildDetailResponse(ExportReceipt receipt) {
        List<ExportReceiptDetail> details = exportReceiptDetailRepository
                .findByExportReceiptIdOrderByIdAsc(receipt.getId());
        Map<Long, Integer> inventoryByProductId = loadInventoryByProductId(details,
                receipt.getWarehouse() != null ? receipt.getWarehouse().getId() : null);
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

    private Warehouse validateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) {
            throw new BadRequestException("Kho hàng không hoạt động.");
        }
        return warehouse;
    }

    private Partner validatePartner(Long partnerId) {
        if (partnerId == null)
            return null;
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại."));
        if (partner.getStatus() != PartnerStatus.HOAT_DONG) {
            throw new BadRequestException("Khách hàng không hoạt động.");
        }
        return partner;
    }

    private List<ExportReceiptDetail> buildDetailsAndValidateInventory(List<ExportReceiptDetailRequest> detailRequests,
            Long warehouseId) {
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
            InventoryLevel inventory = inventoryLevelRepository
                    .findByProductIdAndWarehouseId(product.getId(), warehouseId)
                    .orElseThrow(() -> new BadRequestException(
                            "Sản phẩm " + product.getName() + " không có trong kho này."));

            if (req.getQuantity() > inventory.getQuantity()) {
                throw new BadRequestException("Sản phẩm " + product.getName() + " chỉ còn " + inventory.getQuantity()
                        + " trong kho, không đủ xuất " + req.getQuantity() + ".");
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
