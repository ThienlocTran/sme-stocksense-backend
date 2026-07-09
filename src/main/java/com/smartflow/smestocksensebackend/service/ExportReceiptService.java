package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import org.springframework.data.domain.Pageable;

public interface ExportReceiptService {
    ExportReceiptPageResponse listPendingApproval(String status, Pageable pageable);

    ExportReceiptDetailResponse getDetail(Long receiptId);

    ExportReceiptDetailResponse approve(Long receiptId);
}
