package com.smartflow.smestocksensebackend.dto.response;

import java.time.LocalDate;

public record InventoryMovementPointResponse(
        LocalDate date,
        Long inboundQuantity,
        Long outboundQuantity
) {
}
