package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.forecast.DriftHistoryResponse;
import com.smartflow.smestocksensebackend.service.ForecastDriftHistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/ai/drift-history")
public class AiDriftHistoryController {

    private final ForecastDriftHistoryService forecastDriftHistoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PageResponse<DriftHistoryResponse> list(
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String warehouse,
            @RequestParam(required = false) Boolean retrainNeeded,
            @RequestParam(required = false) Boolean targetRetrainNeeded,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(forecastDriftHistoryService.list(product, warehouse, retrainNeeded,
                targetRetrainNeeded, detectedFrom, detectedTo,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "detectedAt"))));
    }
}
