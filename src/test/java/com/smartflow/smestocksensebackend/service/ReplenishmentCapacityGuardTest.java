package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.replenishment.WarehouseCapacityAvailability;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplenishmentCapacityGuardTest {

    private final ReplenishmentCapacityGuard guard = new ReplenishmentCapacityGuard();

    @Test
    void enoughCapacityKeepsRawSuggestion() {
        var result = guard.apply(40, availability(100));

        assertEquals(40, result.rawSuggestedQty());
        assertEquals(40, result.suggestedQty());
        assertFalse(result.capacityLimited());
        assertEquals(0, result.capacityShortfallQty());
    }

    @Test
    void insufficientCapacityCapsSuggestionAndShowsShortfall() {
        var result = guard.apply(500, availability(320));

        assertEquals(500, result.rawSuggestedQty());
        assertEquals(320, result.suggestedQty());
        assertTrue(result.capacityLimited());
        assertEquals(180, result.capacityShortfallQty());
    }

    @Test
    void noCapacityReturnsZeroSuggestion() {
        var result = guard.apply(50, availability(0));

        assertEquals(0, result.suggestedQty());
        assertTrue(result.capacityLimited());
        assertEquals(50, result.capacityShortfallQty());
    }

    @Test
    void zeroRawDoesNotManufactureRecommendation() {
        var result = guard.apply(0, availability(100));

        assertEquals(0, result.suggestedQty());
        assertFalse(result.capacityLimited());
        assertEquals(0, result.capacityShortfallQty());
    }

    @Test
    void negativeRawCannotProduceNegativeRecommendation() {
        var result = guard.apply(-5, availability(100));

        assertEquals(0, result.rawSuggestedQty());
        assertEquals(0, result.suggestedQty());
    }

    private WarehouseCapacityAvailability availability(Integer maxUnits) {
        return new WarehouseCapacityAvailability(
                new BigDecimal("100.000"),
                new BigDecimal("40.000"),
                new BigDecimal("60.000"),
                maxUnits,
                null
        );
    }
}
