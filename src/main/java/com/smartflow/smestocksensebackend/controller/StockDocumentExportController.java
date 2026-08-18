package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.service.StockDocumentExportService;
import com.smartflow.smestocksensebackend.service.document.GeneratedDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StockDocumentExportController {

    private final StockDocumentExportService stockDocumentExportService;

    @GetMapping("/api/import-receipts/{id:\\d+}/export/pdf")
    public ResponseEntity<byte[]> exportImportPdf(@PathVariable Long id) {
        return buildDownload(stockDocumentExportService.exportImportReceiptPdf(id));
    }

    @GetMapping("/api/import-receipts/{id:\\d+}/export/excel")
    public ResponseEntity<byte[]> exportImportExcel(@PathVariable Long id) {
        return buildDownload(stockDocumentExportService.exportImportReceiptExcel(id));
    }

    @GetMapping("/api/export-receipts/{id:\\d+}/export/pdf")
    public ResponseEntity<byte[]> exportExportPdf(@PathVariable Long id) {
        return buildDownload(stockDocumentExportService.exportExportReceiptPdf(id));
    }

    @GetMapping("/api/export-receipts/{id:\\d+}/export/excel")
    public ResponseEntity<byte[]> exportExportExcel(@PathVariable Long id) {
        return buildDownload(stockDocumentExportService.exportExportReceiptExcel(id));
    }

    private ResponseEntity<byte[]> buildDownload(GeneratedDocument document) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + document.filename() + "\"")
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }
}
