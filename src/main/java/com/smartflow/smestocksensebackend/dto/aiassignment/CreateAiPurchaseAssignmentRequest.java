package com.smartflow.smestocksensebackend.dto.aiassignment;

import com.smartflow.smestocksensebackend.entity.SalesHistorySource;

public record CreateAiPurchaseAssignmentRequest(
        Long productId,
        Long warehouseId,
        Short horizonDays,
        Integer aiSuggestedQuantity,
        Integer requestedQuantity,
        Long receiverId,
        String content,
        Long modelMetadataId,
        SalesHistorySource source
) {
    public CreateAiPurchaseAssignmentRequest(Long productId, Long warehouseId, Short horizonDays,
            Integer aiSuggestedQuantity, Integer requestedQuantity, Long receiverId, String content,
            Long modelMetadataId) {
        this(productId, warehouseId, horizonDays, aiSuggestedQuantity, requestedQuantity, receiverId, content,
                modelMetadataId, null);
    }
}
