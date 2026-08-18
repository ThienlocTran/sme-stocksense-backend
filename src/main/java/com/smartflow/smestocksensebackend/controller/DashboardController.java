package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;
import com.smartflow.smestocksensebackend.dto.response.InventoryMovementPointResponse;
import com.smartflow.smestocksensebackend.dto.response.StockHealthResponse;
import com.smartflow.smestocksensebackend.dto.response.WarehouseDistributionResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import com.smartflow.smestocksensebackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> overview(
            @AuthenticationPrincipal Employee employee
    ) {
        return ResponseEntity.ok(
                dashboardService.getOverview(employee)
        );
    }

    @GetMapping("/inventory-movement")
    public ResponseEntity<List<InventoryMovementPointResponse>> inventoryMovement(
            @AuthenticationPrincipal Employee employee,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId
    ) {
        return ResponseEntity.ok(dashboardService.getInventoryMovement(employee, from, to, warehouseId));
    }

    @GetMapping("/stock-health")
    public ResponseEntity<StockHealthResponse> stockHealth(
            @AuthenticationPrincipal Employee employee
    ) {
        return ResponseEntity.ok(dashboardService.getStockHealth(employee));
    }

    @GetMapping("/warehouse-distribution")
    public ResponseEntity<List<WarehouseDistributionResponse>> warehouseDistribution(
            @AuthenticationPrincipal Employee employee
    ) {
        return ResponseEntity.ok(dashboardService.getWarehouseDistribution(employee));
    }
}
