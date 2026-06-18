package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;

public interface ImportReceiptService {

    ImportReceiptResponse createDraft(CreateImportReceiptRequest request);

    ImportReceiptItemResponse addItem(Long receiptId, AddImportReceiptItemRequest request);

    ImportReceiptDraftResponse saveDraft(Long receiptId, SaveImportReceiptDraftRequest request);

    ImportReceiptDraftResponse updateEditable(Long receiptId, SaveImportReceiptDraftRequest request);

    ImportReceiptDraftResponse cancelDraft(Long receiptId);

    ImportReceiptDraftResponse submitForApproval(Long receiptId);
}
