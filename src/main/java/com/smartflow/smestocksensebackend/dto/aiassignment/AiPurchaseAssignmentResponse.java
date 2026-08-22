package com.smartflow.smestocksensebackend.dto.aiassignment;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestEmailStatus;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestStatus;

import java.time.LocalDateTime;

public record AiPurchaseAssignmentResponse(
        Long id,
        String code,
        Long productId,
        String productCode,
        String productName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Short horizonDays,
        Integer aiSuggestedQuantity,
        Integer requestedQuantity,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
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
                request.getProduct().getCode(),
                request.getProduct().getName(),
                request.getWarehouse().getId(),
                request.getWarehouse().getCode(),
                request.getWarehouse().getName(),
                request.getHorizonDays(),
                request.getAiSuggestedQuantity(),
                request.getRequestedQuantity(),
                request.getSender().getId(),
                request.getSender().getFullName(),
                request.getReceiver().getId(),
                request.getReceiver().getFullName(),
                request.getContent(),
                request.getStatus(),
                request.getEmailStatus(),
                request.getModelMetadata().getId(),
                request.getImportReceipt() != null ? request.getImportReceipt().getId() : null,
                request.getCreatedAt()
        );
    }
}
