package com.smartflow.smestocksensebackend.dto.replenishment;

import java.math.BigDecimal;

public record HorizonDemand(
        Long modelMetadataId,
        Integer modelVersion,
        Short horizonDays,
        BigDecimal forecastDemand
) {
}
