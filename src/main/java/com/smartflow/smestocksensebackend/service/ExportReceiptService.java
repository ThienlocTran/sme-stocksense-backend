package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptResponse;
import org.springframework.data.domain.Pageable;

public interface ExportReceiptService {
    ExportReceiptPageResponse listPendingApproval(String status, Pageable pageable);

    ExportReceiptDetailResponse getDetail(Long receiptId);

    ExportReceiptDetailResponse approve(Long receiptId);

    ExportReceiptResponse createDraft(ExportReceiptDraftRequest request);

    ExportReceiptResponse updateDraft(Long id, ExportReceiptDraftRequest request);
    void cancelDraft(Long id);
    ExportReceiptResponse submitForApproval(Long id, com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptSubmitRequest request);
}
