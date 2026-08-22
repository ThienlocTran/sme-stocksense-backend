package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.forecast.ForecastAvailabilityResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.service.ForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecasts")
@RequiredArgsConstructor
public class ForecastAvailabilityController {

    private final ForecastService forecastService;

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ForecastAvailabilityResponse getAvailability(
            @RequestParam(defaultValue = "EXTERNAL_STORE_ITEM") SalesHistorySource source) {
        return forecastService.getAvailability(source);
    }
}
