package com.smartflow.smestocksensebackend.dto.dashboard;

import java.time.LocalDate;

public interface InventoryMovementProjection {
    LocalDate getDate();

    Long getInboundQuantity();

    Long getOutboundQuantity();
}
