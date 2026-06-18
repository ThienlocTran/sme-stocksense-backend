package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;

public interface ImportReceiptService {

    ImportReceiptResponse createDraft(CreateImportReceiptRequest request);
}
