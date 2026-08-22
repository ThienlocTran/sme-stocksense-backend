package com.smartflow.smestocksensebackend.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ReorderQuantityCalculator {

    public int rawSuggestedQuantity(Integer currentStock, Integer effectiveMinStock, BigDecimal forecastDemand) {
        int current = nonNegative(currentStock);
        int minimum = nonNegative(effectiveMinStock);
        int demand = nonNegativeCeiling(forecastDemand);
        return Math.max(0, demand + minimum - current);
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private int nonNegativeCeiling(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return value.setScale(0, RoundingMode.CEILING).intValue();
    }
}
