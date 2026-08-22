package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.replenishment.CapacityGuardResult;
import com.smartflow.smestocksensebackend.dto.replenishment.WarehouseCapacityAvailability;
import org.springframework.stereotype.Component;

@Component
public class ReplenishmentCapacityGuard {

    public CapacityGuardResult apply(Integer rawSuggestedQty, WarehouseCapacityAvailability availability) {
        int raw = Math.max(0, rawSuggestedQty == null ? 0 : rawSuggestedQty);
        int maxAdditional = Math.max(0, availability.maxAdditionalUnitsByCapacity() == null
                ? 0 : availability.maxAdditionalUnitsByCapacity());
        int suggested = Math.min(raw, maxAdditional);
        boolean limited = suggested < raw;
        return new CapacityGuardResult(
                raw,
                suggested,
                limited,
                raw - suggested,
                maxAdditional,
                availability.warehouseCapacityM3(),
                availability.warehouseOccupiedM3(),
                availability.warehouseAvailableM3(),
                availability.configurationWarning()
        );
    }
}
