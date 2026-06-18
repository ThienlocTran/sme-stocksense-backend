package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptAmountCalculator;
import com.smartflow.smestocksensebackend.domain.inbound.ImportReceiptItemValidator;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
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
import com.smartflow.smestocksensebackend.service.ImportReceiptCodeGenerator;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
        Employee actor = currentEmployee();
        if (actor.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        ImportReceipt receipt = importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new NotFoundException("Phieu nhap khong ton tai."));
        ensureCanSaveDraft(actor, receipt);
        if (receipt.getStatus() != ImportReceiptStatus.NHAP) {
            throw new ConflictException("Chi duoc luu phieu nhap o trang thai NHAP.");
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

    private boolean isDuplicateImportReceiptDetailException(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.contains(IMPORT_RECEIPT_DETAIL_UNIQUE_INDEX);
    }
}
