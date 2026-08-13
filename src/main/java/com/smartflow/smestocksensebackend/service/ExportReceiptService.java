package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inbound.CancelReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.RejectExportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptHistoryResponse;

public interface ExportReceiptService {
    ExportReceiptPageResponse listPendingApproval(String status, Pageable pageable);

    ExportReceiptDetailResponse getDetail(Long receiptId);

    ExportReceiptDetailResponse approve(Long receiptId);

    ExportReceiptDetailResponse complete(Long receiptId);

    ExportReceiptDetailResponse reject(Long receiptId, RejectExportReceiptRequest request);

    ExportReceiptResponse createDraft(ExportReceiptDraftRequest request);

    ExportReceiptResponse updateDraft(Long id, ExportReceiptDraftRequest request);

    void cancelDraft(Long id);

    ExportReceiptDetailResponse cancel(Long id, CancelReceiptRequest request);

    ExportReceiptResponse submitForApproval(Long id,
            com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptSubmitRequest request);

    // ponytail: Truyền thẳng param vào hàm thay vì tạo FilterRequest class.
    org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listReceipts(
            String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listMyReceipts(
            String status, java.time.LocalDate fromDate, java.time.LocalDate toDate, Long warehouseId, String code, org.springframework.data.domain.Pageable pageable);

    ExportReceiptResponse getReceiptDetails(Long id);

    List<ExportReceiptHistoryResponse> getHistory(Long id);
}
