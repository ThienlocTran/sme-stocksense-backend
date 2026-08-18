package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.service.document.GeneratedDocument;

public interface StockDocumentExportService {

    GeneratedDocument exportImportReceiptPdf(Long receiptId);

    GeneratedDocument exportImportReceiptExcel(Long receiptId);

    GeneratedDocument exportExportReceiptPdf(Long receiptId);

    GeneratedDocument exportExportReceiptExcel(Long receiptId);
}
