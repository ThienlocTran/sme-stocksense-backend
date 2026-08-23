package com.smartflow.smestocksensebackend.dto.aiassignment;

public record CreateAiPurchaseAssignmentRequest(
        Long productId,
        Long warehouseId,
        Short horizonDays,
        Integer aiSuggestedQuantity,
        Integer requestedQuantity,
        Long receiverId,
        String content,
        Long modelMetadataId
) {
}
