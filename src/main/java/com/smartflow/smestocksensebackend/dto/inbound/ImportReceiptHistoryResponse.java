package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.ImportReceiptHistory;

import java.time.LocalDateTime;

public record ImportReceiptHistoryResponse(
        Long id,
        Long receiptId,
        Long actorId,
        String actorName,
        String action,
        String note,
        LocalDateTime createdAt
) {
    public static ImportReceiptHistoryResponse from(ImportReceiptHistory history) {
        Long actorId = history.getActor() != null ? history.getActor().getId() : null;
        String actorName = history.getActor() != null ? history.getActor().getFullName() : null;
        return new ImportReceiptHistoryResponse(
                history.getId(),
                history.getDocument().getId(),
                actorId,
                actorName,
                history.getAction(),
                history.getNote(),
                history.getCreatedAt()
        );
    }
}
