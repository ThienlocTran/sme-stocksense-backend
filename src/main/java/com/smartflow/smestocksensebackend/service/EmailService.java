package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.DiscrepancyReport;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;

public interface EmailService {
    void sendImportReceiptSubmitted(ImportReceipt receipt);
    void sendImportReceiptApproved(ImportReceipt receipt, boolean isLevel1, boolean isFullyApproved);
    void sendImportReceiptRejected(ImportReceipt receipt, String reason);
    void sendDiscrepancyReportSubmitted(DiscrepancyReport report);
    void sendDiscrepancyReportApproved(DiscrepancyReport report);
    void sendDiscrepancyReportRejected(DiscrepancyReport report, String reason);
    void sendExportReceiptSubmitted(ExportReceipt receipt);
    void sendExportReceiptApproved(ExportReceipt receipt);
    void sendExportReceiptRejected(ExportReceipt receipt, String reason);
    void sendImportReceiptCompleted(ImportReceipt receipt);
    void sendExportReceiptCompleted(ExportReceipt receipt);
}
