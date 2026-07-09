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

    ExportReceiptResponse submitForApproval(Long id,
            com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptSubmitRequest request);

    // ponytail: Truyền thẳng param vào hàm thay vì tạo FilterRequest class.
    org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listReceipts(
            String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listMyReceipts(
            String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code, org.springframework.data.domain.Pageable pageable);

    ExportReceiptResponse getReceiptDetails(Long id);
}
