package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptStatePolicy;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportReceiptServiceImpl implements ImportReceiptService {

    private static final int MAX_CODE_ATTEMPTS = 3;
    private static final String IMPORT_RECEIPT_DETAIL_UNIQUE_INDEX = "chi_tiet_phieu_nhap_phieu_nhap_id_san_pham_id_idx";

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

    @Override
    @Transactional
    public ImportReceiptDraftResponse saveDraft(Long receiptId, SaveImportReceiptDraftRequest request) {
        return updateReceipt(receiptId, request, false);
    }

    @Override
    @Transactional
    public ImportReceiptDraftResponse updateEditable(Long receiptId, SaveImportReceiptDraftRequest request) {
        return updateReceipt(receiptId, request, true);
    }

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
            List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentId(receiptId);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

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
            throw new ConflictException("Chi duoc gui duyet phieu nhap o trang thai NHAP.");
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
            receipt.setSubmittedBy(actor);
            receipt.setSubmittedAt(LocalDateTime.now());
            ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);
            return ImportReceiptDraftResponse.from(savedReceipt, details);
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("Phieu nhap da duoc cap nhat boi request khac.");
        }
    }

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

    private Warehouse validateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hang khong ton tai."));
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) {
            throw new BadRequestException("Kho hang khong hoat dong.");
        }
        return warehouse;
    }

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

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        return employee;
    }

    private String normalizeOptional(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim();
    }

    private void ensureCanAddItem(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen them san pham vao phieu nhap.");
    }

    private void ensureCanSaveDraft(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen luu phieu nhap.");
    }

    private void ensureCanCancelDraft(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen huy phieu nhap.");
    }

    private void ensureCanSubmitForApproval(Employee actor, ImportReceipt receipt) {
        ensureCanModifyDraft(actor, receipt, "Khong co quyen gui duyet phieu nhap.");
    }

    private void ensureCanListOwnReceipts(Employee actor) {
        RoleCode roleCode = actor.getRole() != null ? actor.getRole().getCode() : null;
        if (roleCode != RoleCode.ADMIN && roleCode != RoleCode.EMPLOYEE) {
            throw new MissingRoleException("Khong co quyen xem danh sach phieu nhap ca nhan.");
        }
    }

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

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentId(receiptId);
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

        List<DiscrepancyReportDetail> reportDetails = new ArrayList<>();
        for (ImportReceiptDetail diff : discrepancyDetails) {
            List<CreateDiscrepancyReportItemRequest> items =
                    request.getItems() != null ? request.getItems() : List.of();
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
     * Lấy phiếu -> Update kiểm hàng (T100) -> Tăng tồn kho (T102) -> Ghi log (T103) -> Đổi status phiếu sang HOAN_THANH.
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

        // 3. Tăng tồn kho (T102) & Ghi log (T103) cho từng dòng sản phẩm
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

        // 4. Đổi status phiếu sang HOAN_THANH
        receipt.setStatus(ImportReceiptStatus.HOAN_THANH);
        receipt.setCompletedBy(actor);
        receipt.setCompletedAt(LocalDateTime.now());
        receipt.setUpdatedAt(LocalDateTime.now());

        ImportReceipt savedReceipt = importReceiptRepository.saveAndFlush(receipt);

        return ImportReceiptDraftResponse.from(savedReceipt, details);
    }

    private boolean isDuplicateImportReceiptDetailException(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.contains(IMPORT_RECEIPT_DETAIL_UNIQUE_INDEX);
    }
}
