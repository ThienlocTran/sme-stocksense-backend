package com.smartflow.smestocksensebackend.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesHistoryReadResponse(
        LocalDate date,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Integer quantity,
        BigDecimal averageSellingPrice,
        String source) {
}
