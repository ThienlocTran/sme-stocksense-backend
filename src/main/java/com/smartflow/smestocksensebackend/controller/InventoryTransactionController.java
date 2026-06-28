package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryTransactionResponse;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Validated
@RequestMapping("/api/inventory/transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private static final Logger log = LoggerFactory.getLogger(InventoryTransactionController.class);
    private final InventoryTransactionService inventoryTransactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public Page<InventoryTransactionResponse> searchTransactions(
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
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            // expected format: property,asc|desc
            String[] parts = sort.split(",");
            if (parts.length == 2) {
                Sort.Direction dir = parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortObj = Sort.by(dir, parts[0]);
            } else {
                sortObj = Sort.by(Sort.Direction.DESC, sort);
            }
        }

        log.debug("InventoryTransactionController.searchTransactions entered, authentication={}",
                SecurityContextHolder.getContext().getAuthentication());
        Pageable pageable = PageRequest.of(page, size, sortObj);

        InventoryTransactionType type = null;
        if (transactionType != null && !transactionType.isBlank()) {
            try {
                type = InventoryTransactionType.valueOf(transactionType);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return inventoryTransactionService.searchTransactions(keyword, productId, warehouseId, type, createdById, from,
                to,
                pageable);
    }
}
