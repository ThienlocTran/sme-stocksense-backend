package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;
import com.smartflow.smestocksensebackend.service.InventoryAdjustmentService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService service;

    @PostMapping("/api/inventory-counts/{countId}/adjustment")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public InventoryAdjustmentResponse getOrCreateDraft(@PathVariable @Positive Long countId) {
        return service.getOrCreateDraft(countId);
    }

    @GetMapping("/api/inventory-counts/{countId}/adjustment")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public InventoryAdjustmentResponse getByCount(@PathVariable @Positive Long countId) {
        return service.getByInventoryCountId(countId);
    }

    @GetMapping("/api/inventory-adjustments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public InventoryAdjustmentResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @PostMapping("/api/inventory-adjustments/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public InventoryAdjustmentResponse submit(@PathVariable @Positive Long id) {
        return service.submit(id);
    }
}
