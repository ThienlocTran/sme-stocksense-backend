package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
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

    /**
     * API kiểm hàng thực tế cho phiếu nhập kho (T100).
     * Ghi nhận số lượng thực tế kiểm đếm, tình trạng vật lý và hạn sử dụng của từng sản phẩm.
     * Đối chiếu số lượng thực tế với số lượng trên chứng từ gốc để phân loại khớp hoặc chênh lệch.
     *
     * @param receiptId ID của phiếu nhập kho cần kiểm hàng
     * @param request DTO chứa danh sách thông tin kiểm hàng chi tiết
     * @return Thông tin chi tiết phiếu nhập sau khi cập nhật kết quả kiểm đếm
     */
    @PutMapping("/{receiptId}/inspect")
    public ImportReceiptDraftResponse inspect(
            @PathVariable Long receiptId,
            @Valid @RequestBody InspectImportReceiptRequest request
    ) {
        return importReceiptService.inspectReceipt(receiptId, request);
    }

    /**
     * API lập biên bản chênh lệch nhập kho (T101).
     * Được gọi khi phiếu nhập có các sản phẩm chênh lệch sau bước kiểm hàng.
     * Tự động lọc ra các sản phẩm lệch, yêu cầu người lập nhập lý do và hướng xử lý đề xuất.
     *
     * @param receiptId ID của phiếu nhập kho cần lập biên bản chênh lệch
     * @param request DTO chứa ghi chú biên bản và thông tin chi tiết xử lý chênh lệch của các sản phẩm
     * @return Biên bản chênh lệch đã lập thành công
     */
    @PostMapping("/{receiptId}/discrepancy-report")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscrepancyReportResponse createDiscrepancyReport(
            @PathVariable Long receiptId,
            @Valid @RequestBody CreateDiscrepancyReportRequest request
    ) {
        return importReceiptService.createDiscrepancyReport(receiptId, request);
    }

    /**
     * API hoàn tất nhập kho (T104).
     * Bọc toàn bộ các khâu (kiểm hàng, tăng tồn, ghi log, đổi trạng thái) trong 1 giao dịch an toàn (ACID).
     *
     * @param id ID của phiếu nhập kho cần hoàn tất
     * @param request DTO chứa thông tin kiểm hàng thực nhận
     * @return Thông tin phiếu nhập kho sau khi hoàn tất
     */
    @PutMapping("/{id}/hoan-tat")
    public ImportReceiptDraftResponse complete(
            @PathVariable Long id,
            @Valid @RequestBody InspectImportReceiptRequest request
    ) {
        return importReceiptService.completeImport(id, request);
    }
}
