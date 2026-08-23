package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.replenishment.ForecastReplenishmentRecommendationResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;

public interface ForecastReplenishmentRecommendationService {

    ForecastReplenishmentRecommendationResponse getRecommendation(Long productId, Long warehouseId, Short horizonDays);

    ForecastReplenishmentRecommendationResponse getRecommendation(Long productId, Long warehouseId, Short horizonDays,
            SalesHistorySource source);
}
