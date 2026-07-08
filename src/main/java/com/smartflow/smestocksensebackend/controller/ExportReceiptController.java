package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export-receipts")
@RequiredArgsConstructor
public class ExportReceiptController {

    private final ExportReceiptService exportReceiptService;

    @GetMapping("/{id:\\d+}")
    public ExportReceiptDetailResponse getDetail(@PathVariable Long id) {
        return exportReceiptService.getDetail(id);
    }

    @GetMapping("/pending-approval")
    public ExportReceiptPageResponse listPendingApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id")));
        return exportReceiptService.listPendingApproval(status, pageable);
    }
}
