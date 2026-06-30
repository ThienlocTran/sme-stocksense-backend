package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.ImportReceiptHistory;

import java.time.LocalDateTime;

public record ImportReceiptHistoryResponse(
        Long id,
        Long receiptId,
        String actorName,
        String action,
        String note,
        LocalDateTime createdAt
) {
    public static ImportReceiptHistoryResponse from(ImportReceiptHistory history) {
        String actorName = history.getActor() != null ? history.getActor().getFullName() : null;
        return new ImportReceiptHistoryResponse(
                history.getId(),
                history.getDocument().getId(),
                actorName,
                history.getAction().name(),
                history.getNote(),
                history.getCreatedAt()
        );
    }
}
