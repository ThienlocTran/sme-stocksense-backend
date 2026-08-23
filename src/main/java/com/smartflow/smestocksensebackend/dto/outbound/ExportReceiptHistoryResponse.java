package com.smartflow.smestocksensebackend.dto.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceiptHistory;
import java.time.LocalDateTime;

public record ExportReceiptHistoryResponse(Long id, Long receiptId, String actorName, String action,
        String note, LocalDateTime createdAt) {
    public static ExportReceiptHistoryResponse from(ExportReceiptHistory history) {
        return new ExportReceiptHistoryResponse(history.getId(), history.getDocument().getId(),
                history.getActor() != null ? history.getActor().getFullName() : null,
                history.getAction(), history.getNote(), history.getCreatedAt());
    }
}
