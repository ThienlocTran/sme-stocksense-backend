package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.replenishment.ForecastReplenishmentRecommendationResponse;
import com.smartflow.smestocksensebackend.dto.replenishment.ReplenishmentSuggestionResponse;
import com.smartflow.smestocksensebackend.service.ForecastReplenishmentRecommendationService;
import com.smartflow.smestocksensebackend.service.ReplenishmentSuggestionService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/replenishment-suggestions")
public class ReplenishmentSuggestionController {
    private final ReplenishmentSuggestionService service;
    private final ForecastReplenishmentRecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PageResponse<ReplenishmentSuggestionResponse> list(
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.listSuggestions(warehouseId, productId, keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/recommendation")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ForecastReplenishmentRecommendationResponse recommendation(
            @RequestParam @Positive Long productId,
            @RequestParam @Positive Long warehouseId,
            @RequestParam @Pattern(regexp = "7|14|30", message = "horizonDays chỉ hỗ trợ 7, 14 hoặc 30.") String horizonDays) {
        return recommendationService.getRecommendation(productId, warehouseId, Short.valueOf(horizonDays));
    }
}
