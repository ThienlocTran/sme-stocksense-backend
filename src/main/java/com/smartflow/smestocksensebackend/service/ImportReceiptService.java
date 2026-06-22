package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptArrivalRequest;
import org.springframework.data.domain.Pageable;

public interface ImportReceiptService {

    ImportReceiptResponse createDraft(CreateImportReceiptRequest request);

    ImportReceiptItemResponse addItem(Long receiptId, AddImportReceiptItemRequest request);

    ImportReceiptDraftResponse saveDraft(Long receiptId, SaveImportReceiptDraftRequest request);

    ImportReceiptDraftResponse updateEditable(Long receiptId, SaveImportReceiptDraftRequest request);

    ImportReceiptDraftResponse cancelDraft(Long receiptId);

    ImportReceiptDraftResponse submitForApproval(Long receiptId);

    ImportReceiptPageResponse listMyReceipts(String status, Pageable pageable);

    ImportReceiptDraftResponse getDetail(Long receiptId);

    ImportReceiptDraftResponse recordArrival(Long receiptId, ImportReceiptArrivalRequest request);
}
