package com.smartflow.smestocksensebackend.dto.aiassignment;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestEmailStatus;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestStatus;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;

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
        Long supplierId,
        String supplierCode,
        String supplierName,
        Short horizonDays,
        Integer aiSuggestedQuantity,
        Integer requestedQuantity,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String receiverEmail,
        String content,
        AiPurchaseRequestStatus status,
        AiPurchaseRequestEmailStatus emailStatus,
        LocalDateTime emailSentAt,
        String emailError,
        SalesHistorySource source,
        Long modelMetadataId,
        Integer modelVersion,
        Long importReceiptId,
        String importReceiptCode,
        ImportReceiptStatus importReceiptStatus,
        LocalDateTime createdAt
) {
    public static AiPurchaseAssignmentResponse from(AiPurchaseRequest request) {
        Partner supplier = request.getProduct().getPartner();
        ImportReceipt importReceipt = request.getImportReceipt();
        return new AiPurchaseAssignmentResponse(
                request.getId(),
                request.getCode(),
                request.getProduct().getId(),
                request.getProduct().getCode(),
                request.getProduct().getName(),
                request.getWarehouse().getId(),
                request.getWarehouse().getCode(),
                request.getWarehouse().getName(),
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getCode() : null,
                supplier != null ? supplier.getName() : null,
                request.getHorizonDays(),
                request.getAiSuggestedQuantity(),
                request.getRequestedQuantity(),
                request.getSender().getId(),
                request.getSender().getFullName(),
                request.getReceiver().getId(),
                request.getReceiver().getFullName(),
                request.getReceiver().getEmail(),
                request.getContent(),
                request.getStatus(),
                request.getEmailStatus(),
                request.getEmailSentAt(),
                request.getEmailError(),
                request.getModelMetadata().getHistorySource(),
                request.getModelMetadata().getId(),
                request.getModelMetadata().getVersion(),
                importReceipt != null ? importReceipt.getId() : null,
                importReceipt != null ? importReceipt.getCode() : null,
                importReceipt != null ? importReceipt.getStatus() : null,
                request.getCreatedAt()
        );
    }
}
