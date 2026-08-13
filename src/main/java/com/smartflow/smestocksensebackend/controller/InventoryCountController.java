package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.inventorycount.*;
import com.smartflow.smestocksensebackend.service.InventoryCountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequiredArgsConstructor @RequestMapping("/api/inventory-counts")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
public class InventoryCountController {
    private final InventoryCountService service;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public InventoryCountResponse create(@Valid @RequestBody InventoryCountRequests.Create request){ return service.create(request); }
    @GetMapping public PageResponse<InventoryCountResponse> list(@RequestParam(required=false) @Positive Long warehouseId,
            @RequestParam(required=false) String status, @RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){ return PageResponse.from(service.list(warehouseId,status,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt")))); }
    @GetMapping("/{id}") public InventoryCountResponse get(@PathVariable @Positive Long id){ return service.get(id); }
    @PutMapping("/{id}/details/{detailId}") public InventoryCountResponse record(@PathVariable @Positive Long id,@PathVariable @Positive Long detailId,@Valid @RequestBody InventoryCountRequests.RecordActual request){ return service.recordActual(id,detailId,request); }
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{id}/finalize") public InventoryCountResponse finalizeCount(@PathVariable @Positive Long id,@Valid @RequestBody InventoryCountRequests.Finalize request){ return service.finalizeCount(id,request); }
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping("/{id}/cancel") public InventoryCountResponse cancel(@PathVariable @Positive Long id,@Valid @RequestBody InventoryCountRequests.Cancel request){ return service.cancel(id,request); }
}
