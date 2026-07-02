package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptArrivalRequest;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptStatePolicy;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptHistoryResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
import com.smartflow.smestocksensebackend.dto.inbound.RejectImportReceiptRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptAction;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ImportReceiptHistory;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptHistoryRepository;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.DiscrepancyReportRepository;
import com.smartflow.smestocksensebackend.repository.DiscrepancyReportDetailRepository;
import com.smartflow.smestocksensebackend.entity.DiscrepancyReport;
import com.smartflow.smestocksensebackend.entity.DiscrepancyReportDetail;
import com.smartflow.smestocksensebackend.service.ImportReceiptCodeGenerator;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.InventoryService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * Service xử lý toàn bộ nghiệp vụ liên quan đến Phiếu Nhập Kho.
 *
 * <p>Luồng trạng thái chuẩn của một phiếu nhập:</p>
 * <pre>
 *  NHAP/TU_CHOI → CHO_DUYET_CAP_1 → CHO_HANG_VE → CHO_KIEM_HANG → [HOAN_THANH]
 *                         ↘ TU_CHOI
 *       ↘ HUY
 * </pre>
 *
 * <p>Phân quyền tổng quát:</p>
 * <ul>
 *   <li>ADMIN: toàn quyền thao tác trên mọi phiếu.</li>
 *   <li>EMPLOYEE: chỉ được thao tác trên phiếu do chính mình tạo.</li>
 *   <li>MANAGER: không được thao tác trực tiếp trên phiếu nhập (chỉ duyệt qua luồng approval).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ImportReceiptServiceImpl implements ImportReceiptService {

    // Số lần tối đa thử sinh mã phiếu nhập mới khi xảy ra trùng lặp
    private static final int MAX_CODE_ATTEMPTS = 3;
    // Tên unique index trong DB để nhận diện lỗi sản phẩm trùng lặp trong cùng một phiếu nhập
    private static final String IMPORT_RECEIPT_DETAIL_UNIQUE_INDEX = "chi_tiet_phieu_nhap_phieu_nhap_id_san_pham_id_idx";

    // Tập trạng thái phiếu nhập đang chờ duyệt (dùng cho luồng duyệt/từ chối T91..T94)
    private static final List<ImportReceiptStatus> PENDING_APPROVAL_STATUSES = List.of(
            ImportReceiptStatus.CHO_DUYET_CAP_1,
            ImportReceiptStatus.CHO_DUYET_CAP_2
    );

    private final ImportReceiptRepository importReceiptRepository;
    private final ImportReceiptDetailRepository importReceiptDetailRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartnerRepository partnerRepository;
    private final ImportReceiptCodeGenerator codeGenerator;
    private final ImportReceiptItemValidator itemValidator;
    private final ImportReceiptAmountCalculator amountCalculator;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final DiscrepancyReportDetailRepository discrepancyReportDetailRepository;
    private final InventoryService inventoryService;
    private final ImportReceiptHistoryRepository importReceiptHistoryRepository;

    @Override
    @Transactional
    public ImportReceiptResponse createDraft(CreateImportReceiptRequest request) {
        Employee creator = currentEmployee();
        if (creator.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) {
            throw new BadRequestException("Kho hàng không hoạt động.");
        }

        Partner supplier = partnerRepository.findById(request.supplierId())
                .orElseThrow(() -> new NotFoundException("Nhà cung cấp không tồn tại."));
        if (supplier.getStatus() != PartnerStatus.HOAT_DONG) {
            throw new BadRequestException("Nhà cung cấp không hoạt động.");
        }
        if (supplier.getType() != PartnerType.NHA_CUNG_CAP && supplier.getType() != PartnerType.CA_HAI) {
            throw new BadRequestException("Đối tác không phải nhà cung cấp.");
        }

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (importReceiptRepository.existsByCodeIgnoreCase(code)) {
                continue;
            }

            ImportReceipt receipt = new ImportReceipt();
            receipt.setCode(code);
            receipt.setWarehouse(warehouse);
            receipt.setSupplier(supplier);
            receipt.setCreatedBy(creator);
            receipt.setStatus(ImportReceiptStatus.NHAP);
            receipt.setTotalAmount(BigDecimal.ZERO);
            receipt.setNote(normalizeOptional(request.note()));

            try {
                return ImportReceiptResponse.from(importReceiptRepository.saveAndFlush(receipt));
            } catch (DataIntegrityViolationException exception) {
                if (attempt == MAX_CODE_ATTEMPTS - 1) {
                    throw new ConflictException("Không thể tạo mã phiếu nhập duy nhất.");
                }
            }
        }

        throw new ConflictException("Không thể tạo mã phiếu nhập duy nhất.");
    }

    /**
     * Thêm một dòng sản phẩm mới vào phiếu nhập đang ở trạng thái NHAP.
     * Mỗi lần thêm sẽ tự động tính lại tổng tiền phiếu.
     * Nếu sản phẩm đã có trong phiếu (vi phạm unique index DB), ném lỗi 409 Conflict.
     */
    @Override
    @Transactional
    public ImportReceiptItemResponse addItem(Long receiptId, AddImportReceiptItemRequest request) {
        itemValidator.validateRequestFields(request);

        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));
        ensureCanAddItem(actor, receipt);
        if (receipt.getStatus() != ImportReceiptStatus.NHAP) {
            throw new ConflictException("Chi duoc them san pham khi phieu nhap o trang thai NHAP.");
        }

        Product product = itemValidator.validateForCreate(receiptId, request);

        ImportReceiptDetail detail = new ImportReceiptDetail();
        detail.setDocument(receipt);
        detail.setProduct(product);
        detail.setExpectedQuantity(request.quantity());
        detail.setExpectedUnitPrice(request.unitPrice());
        detail.setExpectedLineTotal(amountCalculator.calculateLineTotal(request.quantity(), request.unitPrice()));
        detail.setNote(normalizeOptional(request.note()));

        try {
            ImportReceiptDetail savedDetail = importReceiptDetailRepository.saveAndFlush(detail);
            receipt.setTotalAmount(amountCalculator.calculateReceiptTotal(receiptId));
            importReceiptRepository.saveAndFlush(receipt);
            return ImportReceiptItemResponse.from(savedDetail);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateImportReceiptDetailException(exception)) {
                throw itemValidator.duplicateProductException();
            }
            throw exception;
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

    /**
     * Lưu/cập nhật toàn bộ thông tin phiếu nhập khi đang ở trạng thái NHAP.
     * Chỉ cho phép chỉnh sửa khi phiếu ở trạng thái NHAP (nháp mới tạo).
     * Danh sách sản phẩm nếu cung cấp sẽ thay thế toàn bộ các dòng cũ (replace all).
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse saveDraft(Long receiptId, SaveImportReceiptDraftRequest request) {
        return updateReceipt(receiptId, request, false);
    }

    /**
     * Cập nhật phiếu nhập đang ở trạng thái có thể sửa được (NHAP hoặc TU_CHOI).
     * Dùng cho trường hợp phiếu bị từ chối và cần chỉnh sửa để gửi duyệt lại.
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse updateEditable(Long receiptId, SaveImportReceiptDraftRequest request) {
        return updateReceipt(receiptId, request, true);
    }

    /**
     * Hủy phiếu nhập đang ở trạng thái NHAP.
     * Sau khi hủy, phiếu chuyển sang trạng thái HUY và không thể sửa đổi thêm.
     * Ghi nhận thông tin ai hủy và thời điểm hủy.
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse cancelDraft(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));
        ensureCanCancelDraft(actor, receipt);
        if (receipt.getStatus() != ImportReceiptStatus.NHAP) {
            throw new ConflictException("Chi duoc huy phieu nhap o trang thai NHAP.");
        }

        try {
            receipt.setStatus(ImportReceiptStatus.HUY);
            receipt.setCancelledBy(actor);
            receipt.setCancelledAt(LocalDateTime.now());
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            saveHistory(savedReceipt, actor, ImportReceiptAction.HUY, null);
            List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentId(receiptId);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

    // =========================================================================
    // NHÓM 2: LUỒNG DUYỆT PHIẾU NHẬP (NHAP → CHO_DUYET_CAP_1)
    // =========================================================================

    /**
     * Gửi phiếu nhập để bắt đầu quy trình duyệt (chuyển sang CHO_DUYET_CAP_1).
     * Trước khi gửi, hệ thống sẽ:
     * <ol>
     *   <li>Xác nhận kho hàng và nhà cung cấp vẫn còn hoạt động.</li>
     *   <li>Kiểm tra phiếu có ít nhất một sản phẩm hợp lệ.</li>
     *   <li>Tính lại tổng tiền dựa trên dữ liệu hiện tại trong DB.</li>
     * </ol>
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse submitForApproval(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));
        ensureCanSubmitForApproval(actor, receipt);
        if (!ImportReceiptStatePolicy.canTransition(receipt.getStatus(), ImportReceiptStatus.CHO_DUYET_CAP_1)) {
            throw new ConflictException("Chi duoc gui duyet phieu nhap o trang thai NHAP hoac TU_CHOI.");
        }

        Warehouse receiptWarehouse = receipt.getWarehouse();
        Partner receiptSupplier = receipt.getSupplier();
        if (receiptWarehouse == null || receiptWarehouse.getId() == null) {
            throw new NotFoundException("Kho hang khong ton tai.");
        }
        if (receiptSupplier == null || receiptSupplier.getId() == null) {
            throw new NotFoundException("Nha cung cap khong ton tai.");
        }
        validateWarehouse(receiptWarehouse.getId());
        validateSupplier(receiptSupplier.getId());
        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentId(receiptId);
        if (details.isEmpty()) {
            throw new ConflictException("Phieu nhap phai co it nhat mot san pham hop le de gui duyet.");
        }

        BigDecimal totalAmount = validateAndRecalculateDetails(details);

        try {
            receipt.setTotalAmount(totalAmount);
            receipt.setStatus(ImportReceiptStatus.CHO_DUYET_CAP_1);
            receipt.setRejectionReason(null);
            receipt.setSubmittedBy(actor);
            receipt.setSubmittedAt(LocalDateTime.now());
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            saveHistory(savedReceipt, actor, ImportReceiptAction.GUI_DUYET, null);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

    // =========================================================================
    // NHÓM 3: TRA CỨU PHIẾU NHẬP
    // =========================================================================

    /**
     * Lấy danh sách phiếu nhập của nhân viên đang đăng nhập, hỗ trợ phân trang.
     * Nếu không truyền {@code status}, trả về tất cả phiếu.
     * Nếu truyền {@code status}, lọc theo trạng thái tương ứng.
     */
    @Override
    @Transactional(readOnly = true)
    public ImportReceiptPageResponse listMyReceipts(String status, Pageable pageable) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanListOwnReceipts(actor);

        ImportReceiptStatus parsedStatus = parseStatus(status);
        if (parsedStatus == null) {
            return ImportReceiptPageResponse.from(importReceiptRepository
                    .findByCreatedById(actor.getId(), pageable)
                    .map(ImportReceiptSummaryResponse::from));
        }

        return ImportReceiptPageResponse.from(importReceiptRepository
                .findByCreatedByIdAndStatus(actor.getId(), parsedStatus, pageable)
                .map(ImportReceiptSummaryResponse::from));
    }

    /**
     * Lấy chi tiết đầy đủ của một phiếu nhập, bao gồm danh sách sản phẩm sắp xếp theo ID tăng dần.
     * ADMIN có thể xem mọi phiếu; EMPLOYEE chỉ xem được phiếu do mình tạo.
     */
    @Override
    @Transactional(readOnly = true)
    public ImportReceiptDraftResponse getDetail(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanListOwnReceipts(actor);

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        boolean isOwner = receipt.getCreatedBy() != null && receipt.getCreatedBy().getId().equals(actor.getId());
        if (roleCode != RoleCode.ADMIN && !isOwner) {
            throw new MissingRoleException("Khong co quyen xem phieu nhap cua nguoi khac.");
        }

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receiptId);
        return ImportReceiptDraftResponse.from(receipt, details);
    }

    // =========================================================================
    // NHÓM 3B: LUỒNG DUYỆT PHIẾU NHẬP (Approval Flow - T91..T94)
    // =========================================================================

    /**
     * Lấy danh sách phiếu nhập đang chờ duyệt cho quản lý (T91).
     * Chỉ MANAGER/ADMIN được xem. Khi không truyền status thì trả về các phiếu chờ quản lý duyệt
     * (CHO_DUYET_CAP_1 và CHO_DUYET_CAP_2 cũ); khi truyền status thì chỉ chấp nhận các trạng thái này.
     */
    @Override
    @Transactional(readOnly = true)
    public ImportReceiptPageResponse listPendingApproval(String status, Pageable pageable) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ImportReceiptStatus parsedStatus = parseStatus(status);
        if (parsedStatus == null) {
            return ImportReceiptPageResponse.from(importReceiptRepository
                    .findByStatusIn(PENDING_APPROVAL_STATUSES, pageable)
                    .map(ImportReceiptSummaryResponse::from));
        }
        if (!PENDING_APPROVAL_STATUSES.contains(parsedStatus)) {
            throw new BadRequestException("Chi duoc loc theo trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2).");
        }
        return ImportReceiptPageResponse.from(importReceiptRepository
                .findByStatus(parsedStatus, pageable)
                .map(ImportReceiptSummaryResponse::from));
    }

    /**
     * Lấy chi tiết phiếu nhập phục vụ duyệt/từ chối (T92).
     * Khác getDetail: MANAGER/ADMIN xem được chi tiết phiếu của bất kỳ nhân viên nào.
     */
    @Override
    @Transactional(readOnly = true)
    public ImportReceiptDraftResponse getApprovalDetail(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        if (!PENDING_APPROVAL_STATUSES.contains(receipt.getStatus())) {
            throw new ConflictException("Chi duoc xem chi tiet duyet voi phieu nhap o trang thai cho duyet.");
        }

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receiptId);
        return ImportReceiptDraftResponse.from(receipt, details);
    }

    /**
     * Quản lý duyệt phiếu nhập (T93).
     * CHO_DUYET_CAP_1 → CHO_HANG_VE. CHO_DUYET_CAP_2 cũ → CHO_HANG_VE để tương thích dữ liệu.
     * Tuyệt đối không cộng tồn kho ở bước này (chỉ tăng tồn khi hoàn tất - T104).
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse approve(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        ImportReceiptStatus current = receipt.getStatus();
        LocalDateTime now = LocalDateTime.now();

        ImportReceiptStatus nextStatus;
        if (current == ImportReceiptStatus.CHO_DUYET_CAP_1 || current == ImportReceiptStatus.CHO_DUYET_CAP_2) {
            nextStatus = ImportReceiptStatus.CHO_HANG_VE;
            receipt.setLevel2ApprovedBy(actor);
            receipt.setLevel2ApprovedAt(now);
        } else {
            throw new ConflictException("Chi duoc duyet phieu nhap o trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2).");
        }

        // Chốt chặn bằng state machine để đảm bảo transition hợp lệ.
        if (!ImportReceiptStatePolicy.canTransition(current, nextStatus)) {
            throw new ConflictException("Khong the chuyen trang thai phieu nhap tu " + current + " sang " + nextStatus + ".");
        }

        try {
            receipt.setStatus(nextStatus);
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            saveHistory(savedReceipt, actor, ImportReceiptAction.DUYET_CAP_2, null);
            List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receiptId);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

    /**
     * Từ chối phiếu nhập đang chờ duyệt (T94).
     * Áp dụng cho CHO_DUYET_CAP_1/CHO_DUYET_CAP_2; bắt buộc lý do; chuyển sang TU_CHOI.
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse reject(Long receiptId, RejectImportReceiptRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BadRequestException("Ly do tu choi khong duoc de trong.");
        }
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }
        ensureCanApprove(actor);

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        ImportReceiptStatus current = receipt.getStatus();
        if (!PENDING_APPROVAL_STATUSES.contains(current)
                || !ImportReceiptStatePolicy.canTransition(current, ImportReceiptStatus.TU_CHOI)) {
            throw new ConflictException("Chi duoc tu choi phieu nhap o trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2).");
        }

        try {
            receipt.setStatus(ImportReceiptStatus.TU_CHOI);
            receipt.setRejectionReason(request.reason().trim());
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            saveHistory(savedReceipt, actor, ImportReceiptAction.TU_CHOI, request.reason().trim());
            List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receiptId);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImportReceiptHistoryResponse> getHistory(Long receiptId) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        if (!importReceiptRepository.existsById(receiptId)) {
            throw new NotFoundException("Phieu nhap khong ton tai.");
        }

        return importReceiptHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(receiptId)
                .stream()
                .map(ImportReceiptHistoryResponse::from)
                .collect(Collectors.toList());
    }

    private void saveHistory(ImportReceipt receipt, Employee actor, ImportReceiptAction action, String note) {
        if (importReceiptHistoryRepository == null) {
            return;
        }
        ImportReceiptHistory history = new ImportReceiptHistory();
        history.setDocument(receipt);
        history.setActor(actor);
        history.setAction(action);
        history.setNote(note);
        importReceiptHistoryRepository.save(history);
    }

    // =========================================================================
    // NHÓM 4: PHƯƠNG THỨC PRIVATE DÙNG CHUNG (Helper Methods)
    // =========================================================================

    /**
     * Xử lý logic chung cho saveDraft và updateEditable.
     * @param allowRejected nếu {@code true}, cho phép sửa cả phiếu ở trạng thái TU_CHOI.
     */
    private ImportReceiptDraftResponse updateReceipt(
            Long receiptId,
            SaveImportReceiptDraftRequest request,
            boolean allowRejected
    ) {
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));
        ensureCanSaveDraft(actor, receipt);
        boolean canEdit = allowRejected
                ? ImportReceiptStatePolicy.isEditable(receipt.getStatus())
                : receipt.getStatus() == ImportReceiptStatus.NHAP;
        if (!canEdit) {
            throw new ConflictException(allowRejected
                    ? "Chi duoc sua phieu nhap o trang thai NHAP hoac TU_CHOI."
                    : "Chi duoc luu phieu nhap o trang thai NHAP.");
        }

        Warehouse warehouse = validateWarehouse(request.warehouseId());
        Partner supplier = validateSupplier(request.supplierId());
        List<ImportReceiptDetail> replacementDetails = request.items() != null
                ? buildReplacementDetails(receipt, request.items())
                : null;

        try {
            receipt.setWarehouse(warehouse);
            receipt.setSupplier(supplier);
            receipt.setNote(normalizeOptional(request.note()));

            List<ImportReceiptDetail> responseDetails;
            if (replacementDetails != null) {
                importReceiptDetailRepository.deleteByDocumentId(receiptId);
                responseDetails = importReceiptDetailRepository.saveAllAndFlush(replacementDetails);
            } else {
                responseDetails = importReceiptDetailRepository.findByDocumentId(receiptId);
            }

            receipt.setTotalAmount(amountCalculator.calculateReceiptTotal(receiptId));
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            return ImportReceiptDraftResponse.from(savedReceipt, responseDetails);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateImportReceiptDetailException(exception)) {
                throw itemValidator.duplicateProductException();
            }
            throw exception;
        }
    }

    /** Kiểm tra kho hàng tồn tại và đang hoạt động. */
    private Warehouse validateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hang khong ton tai."));
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) {
            throw new BadRequestException("Kho hang khong hoat dong.");
        }
        return warehouse;
    }

    /** Kiểm tra nhà cung cấp tồn tại, đang hoạt động và đúng loại đối tác (NHA_CUNG_CAP hoặc CA_HAI). */
    private Partner validateSupplier(Long supplierId) {
        Partner supplier = partnerRepository.findById(supplierId)
                .orElseThrow(() -> new NotFoundException("Nha cung cap khong ton tai."));
        if (supplier.getStatus() != PartnerStatus.HOAT_DONG) {
            throw new BadRequestException("Nha cung cap khong hoat dong.");
        }
        if (supplier.getType() != PartnerType.NHA_CUNG_CAP && supplier.getType() != PartnerType.CA_HAI) {
            throw new BadRequestException("Doi tac khong phai nha cung cap.");
        }
        return supplier;
    }

    /**
     * Xây dựng danh sách dòng chi tiết phiếu mới từ request, thay thế toàn bộ danh sách cũ.
     * Kiểm tra trùng lặp sản phẩm và validate từng dòng trước khi tạo entity.
     */
    private List<ImportReceiptDetail> buildReplacementDetails(
            ImportReceipt receipt,
            List<SaveImportReceiptDraftItemRequest> items
    ) {
        Set<Long> productIds = new LinkedHashSet<>();
        List<ImportReceiptDetail> details = new ArrayList<>();
        for (SaveImportReceiptDraftItemRequest item : items) {
            if (item == null) {
                throw new BadRequestException("items khong hop le.");
            }
            AddImportReceiptItemRequest itemRequest = item.toAddItemRequest();
            Product product = itemValidator.validateForDraftSave(itemRequest);
            if (!productIds.add(item.productId())) {
                throw itemValidator.duplicateProductException();
            }

            ImportReceiptDetail detail = new ImportReceiptDetail();
            detail.setDocument(receipt);
            detail.setProduct(product);
            detail.setExpectedQuantity(item.quantity());
            detail.setExpectedUnitPrice(item.unitPrice());
            detail.setExpectedLineTotal(amountCalculator.calculateLineTotal(item.quantity(), item.unitPrice()));
            detail.setNote(normalizeOptional(item.note()));
            details.add(detail);
        }
        return details;
    }

    /**
     * Validate lại và tính tổng tiền dựa trên danh sách chi tiết hiện tại trong DB.
     * Được gọi trước khi gửi duyệt để đảm bảo dữ liệu tài chính luôn chính xác.
     */
    private BigDecimal validateAndRecalculateDetails(List<ImportReceiptDetail> details) {
        Set<Long> productIds = new LinkedHashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ImportReceiptDetail detail : details) {
            Product product = detail.getProduct();
            if (product == null || product.getId() == null) {
                throw new BadRequestException("San pham khong ton tai.");
            }
            AddImportReceiptItemRequest itemRequest = new AddImportReceiptItemRequest(
                    product.getId(),
                    detail.getExpectedQuantity(),
                    detail.getExpectedUnitPrice(),
                    detail.getNote()
            );
            itemValidator.validateForDraftSave(itemRequest);
            if (!productIds.add(product.getId())) {
                throw itemValidator.duplicateProductException();
            }

            BigDecimal lineTotal = amountCalculator.calculateLineTotal(
                    detail.getExpectedQuantity(),
                    detail.getExpectedUnitPrice()
            );
            detail.setExpectedLineTotal(lineTotal);
            totalAmount = totalAmount.add(lineTotal);
        }
        return totalAmount;
    }

    /** Lấy thông tin nhân viên đang đăng nhập từ SecurityContext. Ném lỗi 401 nếu chưa xác thực. */
    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        return employee;
    }

    /** Chuẩn hóa trường tùy chọn: trả về {@code null} nếu blank, ngược lại trim whitespace hai đầu. */
    private String normalizeOptional(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim();
    }

    // --- Các wrapper kiểm tra quyền cho từng hành động cụ thể ---

    /** Kiểm tra quyền thêm sản phẩm vào phiếu nhập. */
    private void ensureCanAddItem(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen them san pham vao phieu nhap.");
    }

    /** Kiểm tra quyền lưu/cập nhật phiếu nhập nháp. */
    private void ensureCanSaveDraft(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen luu phieu nhap.");
    }

    /** Kiểm tra quyền hủy phiếu nhập. */
    private void ensureCanCancelDraft(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen huy phieu nhap.");
    }

    /** Kiểm tra quyền gửi phiếu nhập lên luồng duyệt. */
    private void ensureCanSubmitForApproval(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen gui duyet phieu nhap.");
    }

    /** Kiểm tra nhân viên có quyền xem danh sách phiếu nhập cá nhân (chỉ ADMIN hoặc EMPLOYEE). */
    private void ensureCanListOwnReceipts(Employee actor) {
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException("Khong co quyen xem danh sach phieu nhap ca nhan.");
        }
    }

    /** Kiểm tra nhân viên có quyền duyệt/từ chối phiếu nhập (chỉ ADMIN hoặc MANAGER). */
    private void ensureCanApprove(Employee actor) {
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.MANAGER) {
            throw new MissingRoleException("Khong co quyen duyet phieu nhap.");
        }
    }

    /** Chuyển đổi chuỗi status từ request thành enum {@link ImportReceiptStatus}. Trả về null nếu không truyền. */
    private ImportReceiptStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ImportReceiptStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("status khong hop le.");
        }
    }

    /**
     * Luật kiểm tra quyền chung cho mọi thao tác ghi trên phiếu nhập.
     * ADMIN được phép mọi thứ; EMPLOYEE chỉ được sửa phiếu do mình tạo.
     */
    private void ensureCanModifyDraft(Employee actor, ImportReceipt receipt, String missingRoleMessage) {
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode == RoleCode.ADMIN) {
            return;
        }
        if (roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException(missingRoleMessage);
        }

        Employee creator = receipt.getCreatedBy();
        if (creator == null || !actor.getId().equals(creator.getId())) {
            throw new MissingRoleException("Khong co quyen sua phieu nhap cua nguoi khac.");
        }
    }

    /**
     * Thực hiện kiểm hàng thực tế cho phiếu nhập kho (T100).
     * Ghi nhận số lượng thực đếm, tình trạng sản phẩm và đối chiếu khớp/lệch với chứng từ gốc.
     * Chỉ áp dụng cho phiếu nhập từ Nhà cung cấp đang ở trạng thái CHO_KIEM_HANG.
     * Cập nhật trạng thái dòng là KHOP hoặc CHENH_LECH.
     *
     * @param receiptId ID của phiếu nhập kho
     * @param request Yêu cầu kiểm hàng chứa danh sách sản phẩm và số lượng thực tế kiểm đếm
     * @return Kết quả kiểm hàng và chi tiết phiếu nhập
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse inspectReceipt(Long receiptId, InspectImportReceiptRequest request) {
        if (request == null || request.items() == null) {
            throw new BadRequestException("Yeu cau kiem hang khong hop le.");
        }
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException("Khong co quyen thuc hien kiem hang.");
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        // Kiểm tra quyền sở hữu: EMPLOYEE chỉ được kiểm hàng phiếu do chính mình tạo
        if (roleCode == RoleCode.EMPLOYEE) {
            Employee creator = receipt.getCreatedBy();
            if (creator == null || !actor.getId().equals(creator.getId())) {
                throw new MissingRoleException("Khong co quyen tac dong vao phieu nhap cua nguoi khac.");
            }
        }

        // Lỗi: Phiếu nhập không ở trạng thái CHO_KIEM_HANG -> Báo lỗi.
        if (receipt.getStatus() != ImportReceiptStatus.CHO_KIEM_HANG) {
            throw new BadRequestException("Phieu nhap khong o trang thai CHO_KIEM_HANG.");
        }

        // Áp dụng khi nhập hàng NCC, không áp dụng đơn khách mua/đặt hàng.
        Partner supplier = receipt.getSupplier();
        if (supplier == null || (supplier.getType() != PartnerType.NHA_CUNG_CAP && supplier.getType() != PartnerType.CA_HAI)) {
            throw new BadRequestException("Chi ap dung kiem hang cho phieu nhap tu nha cung cap.");
        }

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receiptId);
        if (details.isEmpty()) {
            throw new BadRequestException("Phieu nhap khong co san pham nao de kiem hang.");
        }

        /*
         * Chỉ kiểm đếm lưu số lượng thực tế, không thực hiện cộng tồn kho ở bước này.
         * Logic này sẽ được thực hiện khi hoàn thành phiếu nhập kho ở các task sau (T104/T102).
         */
        // Xây dựng Map để tra cứu nhanh chi tiết theo productId (tránh O(n²))
        Map<Long, ImportReceiptDetail> detailByProductId = new HashMap<>();
        for (ImportReceiptDetail d : details) {
            detailByProductId.put(d.getProduct().getId(), d);
        }

        // Track các productId đã được kiểm để phát hiện trùng lặp và kiểm tra đủ dòng
        Set<Long> inspectedProductIds = new HashSet<>();
        for (InspectImportReceiptItemRequest item : request.items()) {
            if (item == null || item.productId() == null || item.actualReceivedQuantity() == null) {
                throw new BadRequestException("Thong tin san pham kiem hang khong hop le.");
            }
            if (item.actualReceivedQuantity() < 0) {
                throw new BadRequestException("So luong thuc nhan khong duoc am.");
            }
            if (!inspectedProductIds.add(item.productId())) {
                throw new BadRequestException("Danh sach san pham kiem hang bi trung san pham ID: " + item.productId() + ".");
            }
            ImportReceiptDetail detail = detailByProductId.get(item.productId());
            if (detail == null) {
                throw new BadRequestException("San pham co ID " + item.productId() + " khong co trong phieu nhap.");
            }

            detail.setActualReceivedQuantity(item.actualReceivedQuantity());
            detail.setPhysicalStatus(normalizeOptional(item.physicalStatus()));
            detail.setExpiryDate(item.expiryDate());

            // Logic đối chiếu khớp/lệch: So sánh số lượng thực nhận và số lượng dự kiến trên chứng từ gốc
            if (detail.getExpectedQuantity().equals(item.actualReceivedQuantity())) {
                detail.setRowStatus("KHOP");
            } else {
                detail.setRowStatus("CHENH_LECH");
            }
        }

        // Bắt buộc kiểm tra đầy đủ: Số dòng request phải bằng số dòng trong phiếu
        if (inspectedProductIds.size() != details.size()) {
            throw new BadRequestException("Yeu cau kiem hang phai bao gom day du tat ca san pham trong phieu nhap.");
        }

        try {
            importReceiptDetailRepository.saveAllAndFlush(details);
            receipt.setUpdatedAt(LocalDateTime.now());
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

    /**
     * Lập biên bản chênh lệch cho phiếu nhập kho (T101).
     * Được gọi khi phiếu nhập có các sản phẩm bị lệch số lượng (CHENH_LECH) sau kiểm hàng.
     * Tự động tạo mã biên bản, lưu thông tin lý do và hướng xử lý đề xuất cho các sản phẩm bị lệch.
     *
     * @param receiptId ID của phiếu nhập kho
     * @param request Yêu cầu lập biên bản chứa ghi chú và các chi tiết xử lý
     * @return Biên bản chênh lệch đã lập thành công
     */
    @Override
    @Transactional
    public DiscrepancyReportResponse createDiscrepancyReport(Long receiptId, CreateDiscrepancyReportRequest request) {
        if (request == null) {
            throw new BadRequestException("Yeu cau lap bien ban khong hop le.");
        }
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException("Khong co quyen lap bien ban.");
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        if (roleCode == RoleCode.EMPLOYEE) {
            Employee creator = receipt.getCreatedBy();
            if (creator == null || !actor.getId().equals(creator.getId())) {
                throw new MissingRoleException("Khong co quyen tac dong vao phieu nhap cua nguoi khac.");
            }
        }

        if (receipt.getStatus() != ImportReceiptStatus.CHO_KIEM_HANG) {
            throw new BadRequestException("Phieu nhap khong o trang thai CHO_KIEM_HANG.");
        }

        if (discrepancyReportRepository.existsByImportReceiptId(receiptId)) {
            throw new ConflictException("Bien ban chenh lech cho phieu nhap nay da ton tai.");
        }

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentId(receiptId);

        // Lọc các dòng bị lệch được đánh dấu ở bước kiểm đếm
        List<ImportReceiptDetail> discrepancyDetails = details.stream()
                .filter(d -> "CHENH_LECH".equals(d.getRowStatus()))
                .toList();

        // Lỗi: Nếu phiếu không có dòng nào chênh lệch mà gọi API -> Báo lỗi.
        if (discrepancyDetails.isEmpty()) {
            throw new BadRequestException("Phieu nhap khong co san pham nao bi chenh lech de lap bien ban.");
        }

        // Biên bản tự động sinh ra dựa vào dữ liệu lệch đã được mark ở bước kiểm đếm
        DiscrepancyReport report = new DiscrepancyReport();
        report.setImportReceipt(receipt);
        report.setCreatedBy(actor);
        report.setReportDate(LocalDateTime.now());
        report.setNote(normalizeOptional(request.getNote()));

        String code = "BBCL-" + receipt.getCode();
        if (discrepancyReportRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Ma bien ban chenh lech " + code + " da ton tai.");
        }
        report.setCode(code);

        // Validate danh sách items: tránh NullPointerException khi item hoặc productId là null
        List<CreateDiscrepancyReportItemRequest> items = request.getItems() != null ? request.getItems() : List.of();
        for (CreateDiscrepancyReportItemRequest item : items) {
            if (item == null || item.getProductId() == null) {
                throw new BadRequestException("Thong tin san pham bien ban khong hop le.");
            }
        }

        List<DiscrepancyReportDetail> reportDetails = new ArrayList<>();
        for (ImportReceiptDetail diff : discrepancyDetails) {
            CreateDiscrepancyReportItemRequest itemReq = items.stream()
                    .filter(i -> i.getProductId().equals(diff.getProduct().getId()))
                    .findFirst()
                    .orElse(null);

            DiscrepancyReportDetail reportDetail = new DiscrepancyReportDetail();
            reportDetail.setReport(report);
            reportDetail.setProduct(diff.getProduct());
            reportDetail.setDocumentQuantity(diff.getExpectedQuantity());
            reportDetail.setActualQuantity(diff.getActualReceivedQuantity());
            reportDetail.setDiscrepancyQuantity(diff.getActualReceivedQuantity() - diff.getExpectedQuantity());
            String reason = itemReq != null ? normalizeOptional(itemReq.getReason()) : null;
            String action = itemReq != null ? normalizeOptional(itemReq.getAction()) : null;
            reportDetail.setReason(reason != null ? reason : "Chua xac dinh");
            reportDetail.setAction(action != null ? action : "Chua xac dinh");
            reportDetails.add(reportDetail);
        }
        report.setDetails(reportDetails);

        DiscrepancyReport savedReport;
        try {
            savedReport = discrepancyReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Bien ban chenh lech cho phieu nhap nay da ton tai.");
        }
        return DiscrepancyReportResponse.from(savedReport);
    }

    /**
     * Hoàn tất phiếu nhập kho (T104).
     * Bọc toàn bộ các khâu vào 1 giao dịch an toàn (ACID):
     * Lấy phiếu -> Update kiểm hàng (T100) -> Đổi status phiếu sang HOAN_THANH -> Tăng tồn kho (T102) -> Ghi log (T103).
     * Bất kỳ lỗi nào xảy ra sẽ kích hoạt Rollback toàn bộ trạng thái của giao dịch này.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportReceiptDraftResponse completeImport(Long receiptId, InspectImportReceiptRequest request) {
        // Ghi chú đậm: **Transaction cốt lõi. Tránh lỗi phiếu chuyển Hoàn Thành nhưng tồn kho bị sai**

        // 1. Lấy phiếu nhập kho
        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));

        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        // 2. Update kiểm hàng (T100)
        // Gọi inspectReceipt để thực hiện validation nghiệp vụ kiểm hàng và cập nhật số lượng thực tế
        inspectReceipt(receiptId, request);

        // Nạp lại chi tiết phiếu nhập sau khi đã được cập nhật kiểm hàng
        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentId(receiptId);

        // 3. Đổi status phiếu sang HOAN_THANH trước khi tăng tồn kho để thỏa guard nghiệp vụ
        receipt.setStatus(ImportReceiptStatus.HOAN_THANH);
        receipt.setCompletedBy(actor);
        receipt.setCompletedAt(LocalDateTime.now());
        receipt.setUpdatedAt(LocalDateTime.now());

        // 4. Tăng tồn kho (T102) & Ghi log (T103) cho từng dòng sản phẩm
        for (ImportReceiptDetail detail : details) {
            if (detail.getActualReceivedQuantity() != null && detail.getActualReceivedQuantity() > 0) {
                inventoryService.increaseInventory(
                        detail.getProduct().getId(),
                        receipt.getWarehouse().getId(),
                        detail.getActualReceivedQuantity(),
                        receipt
                );
            }
        }

        ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);

        return ImportReceiptDraftResponse.from(savedReceipt, details);
    }

    private boolean isDuplicateImportReceiptDetailException(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.contains(IMPORT_RECEIPT_DETAIL_UNIQUE_INDEX);
    }

    /**
     * Ghi nhận ngày hàng về thực tế và cập nhật trạng thái phiếu nhập kho sang CHO_KIEM_HANG.
     * Chỉ những phiếu đang ở trạng thái CHO_HANG_VE mới được phép cập nhật.
     * 
     * @param receiptId ID của phiếu nhập kho
     * @param request DTO chứa ngày hàng về thực tế
     * @return ImportReceiptDraftResponse thông tin chi tiết phiếu sau khi cập nhật
     */
    @Override
    @Transactional
    public ImportReceiptDraftResponse recordArrival(Long receiptId, ImportReceiptArrivalRequest request) {
        // Lấy thông tin nhân viên hiện tại thực hiện request
        Employee actor = currentEmployee();
        // Kiểm tra xem tài khoản nhân viên có đang hoạt động hay không
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        // Kiểm tra phân quyền nghiệp vụ: Chỉ cho phép ADMIN hoặc EMPLOYEE (Nhân viên kho) thực hiện ghi nhận
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException("Không có quyền ghi nhận hàng về.");
        }

        // Truy vấn dữ liệu thực tế từ DB, ném lỗi 404 nếu không tìm thấy phiếu nhập
        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phiếu nhập không tồn tại."));

        /*
         * logic check trạng thái:
         * Chỉ phiếu nhập kho có trạng thái là CHO_HANG_VE mới được phép cập nhật ngày hàng về thực tế
         * và chuyển đổi trạng thái sang CHO_KIEM_HANG.
         * Nếu phiếu nhập đang ở trạng thái khác (ví dụ: NHAP, CHO_DUYET, HOAN_THANH...),
         * hệ thống sẽ chặn lại và quăng lỗi ConflictException.
         */
        if (receipt.getStatus() != ImportReceiptStatus.CHO_HANG_VE) {
            throw new ConflictException("Chỉ được ghi nhận hàng về cho phiếu nhập ở trạng thái chờ hàng về (CHO_HANG_VE).");
        }

        try {
            // Cập nhật ngày hàng về thực tế
            receipt.setActualArrivalDate(request.actualArrivalDate());
            // Cập nhật trạng thái phiếu nhập sang CHO_KIEM_HANG
            receipt.setStatus(ImportReceiptStatus.CHO_KIEM_HANG);

            // Lưu dữ liệu cập nhật xuống PostgreSQL DB thật
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);

            // Lấy danh sách chi tiết các mặt hàng đi kèm phiếu để đóng gói kết quả trả về
            List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receiptId);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            // Xử lý khi có xung đột dữ liệu do ghi đồng thời
            throw new ConflictException("Phiếu nhập đã được cập nhật bởi một phiên làm việc khác.");
        }
    }
}
