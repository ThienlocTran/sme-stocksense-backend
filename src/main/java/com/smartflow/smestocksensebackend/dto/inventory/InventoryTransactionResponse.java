package com.smartflow.smestocksensebackend.dto.inventory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InventoryTransactionResponse {
    private Long id;

    private Long productId;
    private String productCode;
    private String productName;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private String transactionType;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;

    private String note;

    private LocalDateTime createdAt;

    private Long createdById;
    private String createdByName;

    private Long importReceiptId;
    private Long exportReceiptId;
}
