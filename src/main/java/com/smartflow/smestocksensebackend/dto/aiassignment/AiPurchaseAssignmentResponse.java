package com.smartflow.smestocksensebackend.dto.aiassignment;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestEmailStatus;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestStatus;

import java.time.LocalDateTime;

public record AiPurchaseAssignmentResponse(
        Long id,
        String code,
        Long productId,
        Long warehouseId,
        Short horizonDays,
        Integer aiSuggestedQuantity,
        Integer requestedQuantity,
        Long senderId,
        Long receiverId,
        String content,
        AiPurchaseRequestStatus status,
        AiPurchaseRequestEmailStatus emailStatus,
        Long modelMetadataId,
        Long importReceiptId,
        LocalDateTime createdAt
) {
    public static AiPurchaseAssignmentResponse from(AiPurchaseRequest request) {
        return new AiPurchaseAssignmentResponse(
                request.getId(),
                request.getCode(),
                request.getProduct().getId(),
                request.getWarehouse().getId(),
                request.getHorizonDays(),
                request.getAiSuggestedQuantity(),
                request.getRequestedQuantity(),
                request.getSender().getId(),
                request.getReceiver().getId(),
                request.getContent(),
                request.getStatus(),
                request.getEmailStatus(),
                request.getModelMetadata().getId(),
                request.getImportReceipt() != null ? request.getImportReceipt().getId() : null,
                request.getCreatedAt()
        );
    }
}
