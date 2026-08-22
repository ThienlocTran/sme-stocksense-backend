package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.replenishment.ForecastReplenishmentRecommendationResponse;

public interface ForecastReplenishmentRecommendationService {

    ForecastReplenishmentRecommendationResponse getRecommendation(Long productId, Long warehouseId, Short horizonDays);
}
