package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import org.springframework.http.ResponseEntity;
import com.smartflow.smestocksensebackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
