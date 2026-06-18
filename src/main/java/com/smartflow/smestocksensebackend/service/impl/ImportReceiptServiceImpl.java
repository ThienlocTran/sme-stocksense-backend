package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptCodeGenerator;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ImportReceiptServiceImpl implements ImportReceiptService {

    private static final int MAX_CODE_ATTEMPTS = 3;

    private final ImportReceiptRepository importReceiptRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartnerRepository partnerRepository;
    private final ImportReceiptCodeGenerator codeGenerator;

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
}
