package com.smartflow.smestocksensebackend.dto.outbound;

import org.springframework.data.domain.Page;

import java.util.List;

public record ExportReceiptPageResponse(
        List<ExportReceiptSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
    public static ExportReceiptPageResponse from(Page<ExportReceiptSummaryResponse> receiptPage) {
        return new ExportReceiptPageResponse(
                receiptPage.getContent(),
                receiptPage.getNumber(),
                receiptPage.getSize(),
                receiptPage.getTotalElements(),
                receiptPage.getTotalPages());
    }
}
