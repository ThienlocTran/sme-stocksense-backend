package com.smartflow.smestocksensebackend.dto.response;

import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InventoryAlertResponse {
    private Long id;

    private Long productId;
    private String productCode;
    private String productName;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Integer currentQuantity;
    private Integer minStock;

    private InventoryAlertSeverity severity;
    private InventoryAlertStatus status;

    private String note;
    private String handledBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
