package com.smartflow.smestocksensebackend.dto.inbound;

import org.springframework.data.domain.Page;

import java.util.List;

public record ImportReceiptPageResponse(
        List<ImportReceiptSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ImportReceiptPageResponse from(Page<ImportReceiptSummaryResponse> receiptPage) {
        return new ImportReceiptPageResponse(
                receiptPage.getContent(),
                receiptPage.getNumber(),
                receiptPage.getSize(),
                receiptPage.getTotalElements(),
                receiptPage.getTotalPages()
        );
    }
}
