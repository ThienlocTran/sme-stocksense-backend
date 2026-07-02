package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryTransactionResponse;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/inventory/transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransactionController.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "id", "quantity", "quantityBefore", "quantityAfter", "transactionType");
    private final InventoryTransactionService inventoryTransactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PageResponse<InventoryTransactionResponse> searchTransactions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @Positive Long createdById,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {
        Sort sortObj = parseSort(sort);

        log.debug("InventoryTransactionController.searchTransactions entered");
        Pageable pageable = PageRequest.of(page, size, sortObj);

        InventoryTransactionType type = null;
        if (transactionType != null && !transactionType.isBlank()) {
            try {
                type = InventoryTransactionType.valueOf(transactionType.trim().toUpperCase());
            } catch (IllegalArgumentException exception) {
                throw new BadRequestException("Loại giao dịch không hợp lệ: " + transactionType);
            }
        }

        return PageResponse.from(inventoryTransactionService.searchTransactions(keyword, productId, warehouseId, type, createdById, from,
                to,
                pageable));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        if (parts.length > 2) {
            throw new BadRequestException("Tham số sort không hợp lệ.");
        }

        String property = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            throw new BadRequestException("Trường sắp xếp không hợp lệ: " + property);
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            String requestedDirection = parts[1].trim();
            if (requestedDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            } else if (requestedDirection.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            } else {
                throw new BadRequestException("Hướng sắp xếp không hợp lệ: " + requestedDirection);
            }
        }

        return Sort.by(direction, property);
    }
}
