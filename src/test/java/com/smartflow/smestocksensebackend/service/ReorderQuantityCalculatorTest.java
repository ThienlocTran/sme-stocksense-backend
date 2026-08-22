package com.smartflow.smestocksensebackend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReorderQuantityCalculatorTest {

    private final ReorderQuantityCalculator calculator = new ReorderQuantityCalculator();

    @Test
    void enoughStockReturnsZero() {
        assertEquals(0, calculator.rawSuggestedQuantity(100, 10, BigDecimal.valueOf(20)));
    }

    @Test
    void demandPlusMinimumMinusCurrentReturnsRawNeed() {
        assertEquals(40, calculator.rawSuggestedQuantity(20, 10, BigDecimal.valueOf(50)));
    }

    @Test
    void zeroDemandStillRestoresMinimumStock() {
        assertEquals(10, calculator.rawSuggestedQuantity(0, 10, BigDecimal.ZERO));
    }

    @Test
    void currentAboveRequiredReturnsZero() {
        assertEquals(0, calculator.rawSuggestedQuantity(25, 10, BigDecimal.valueOf(5)));
    }

    @Test
    void decimalForecastRoundsUpForIntegerStock() {
        assertEquals(3, calculator.rawSuggestedQuantity(10, 10, BigDecimal.valueOf(2.1)));
    }

    @Test
    void negativeInputCannotProduceNegativeRecommendation() {
        assertEquals(0, calculator.rawSuggestedQuantity(-5, -10, BigDecimal.valueOf(-2.1)));
    }
}
