package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import-receipts")
@RequiredArgsConstructor
public class ImportReceiptController {

    private final ImportReceiptService importReceiptService;

    @GetMapping("/my")
    public ImportReceiptPageResponse listMyReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return importReceiptService.listMyReceipts(status, pageable);
    }

    @GetMapping("/{receiptId}")
    public ImportReceiptDraftResponse getDetail(@PathVariable Long receiptId) {
        return importReceiptService.getDetail(receiptId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImportReceiptResponse createDraft(@Valid @RequestBody CreateImportReceiptRequest request) {
        return importReceiptService.createDraft(request);
    }

    @PostMapping("/{receiptId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportReceiptItemResponse addItem(
            @PathVariable Long receiptId,
            @Valid @RequestBody AddImportReceiptItemRequest request
    ) {
        return importReceiptService.addItem(receiptId, request);
    }

    @PutMapping("/{receiptId}/draft")
    public ImportReceiptDraftResponse saveDraft(
            @PathVariable Long receiptId,
            @Valid @RequestBody SaveImportReceiptDraftRequest request
    ) {
        return importReceiptService.saveDraft(receiptId, request);
    }

    @PutMapping("/{receiptId}")
    public ImportReceiptDraftResponse updateEditable(
            @PathVariable Long receiptId,
            @Valid @RequestBody SaveImportReceiptDraftRequest request
    ) {
        return importReceiptService.updateEditable(receiptId, request);
    }

    @PutMapping("/{receiptId}/cancel")
    public ImportReceiptDraftResponse cancelDraft(@PathVariable Long receiptId) {
        return importReceiptService.cancelDraft(receiptId);
    }

    @PutMapping("/{receiptId}/submit")
    public ImportReceiptDraftResponse submitForApproval(@PathVariable Long receiptId) {
        return importReceiptService.submitForApproval(receiptId);
    }
}
