package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inbound.CancelReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.RejectExportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptHistoryResponse;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.response.outbound.StockAvailabilityResponse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import com.smartflow.smestocksensebackend.service.impl.ExportReceiptItemServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export-receipts")
@RequiredArgsConstructor
public class ExportReceiptController {

    private final ExportReceiptService exportReceiptService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ExportReceiptItemServiceImpl exportReceiptItemService;

    @GetMapping("/{id:\\d+}")
    public ExportReceiptDetailResponse getDetail(@PathVariable Long id) {
        return exportReceiptService.getDetail(id);
    }

    @GetMapping("/pending-approval")
    public ExportReceiptPageResponse listPendingApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        validatePageAndSize(page, size);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id")));
        return exportReceiptService.listPendingApproval(status, pageable);
    }

    @PutMapping("/{id:\\d+}/approve")
    public ExportReceiptDetailResponse approve(@PathVariable Long id) {
        return exportReceiptService.approve(id);
    }

    @PutMapping("/{id:\\d+}/complete")
    public ExportReceiptDetailResponse complete(@PathVariable Long id) {
        return exportReceiptService.complete(id);
    }

    @PutMapping("/{id:\\d+}/reject")
    public ExportReceiptDetailResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectExportReceiptRequest request) {
        return exportReceiptService.reject(id, request);
    }

    @PostMapping("/{id:\\d+}/cancel")
    public ExportReceiptDetailResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelReceiptRequest request) {
        return exportReceiptService.cancel(id, request);
    }

    @PostMapping("/draft")
    public ResponseEntity<ExportReceiptResponse> createDraft(@Valid @RequestBody ExportReceiptDraftRequest request) {
        ExportReceiptResponse response = exportReceiptService.createDraft(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/draft")
    public ResponseEntity<ExportReceiptResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody ExportReceiptDraftRequest request) {
        ExportReceiptResponse response = exportReceiptService.updateDraft(id, request);
        return ResponseEntity.ok(response);
    }

    private void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page must be greater than or equal to 0.");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size must be between 1 and 100.");
        }
    }

    /**
     * API Gửi Phiếu Xuất Kho Chờ Duyệt (T118)
     * Kích hoạt quy trình duyệt 2 cấp.
     *
     * @param id      ID của phiếu xuất cần gửi duyệt.
     * @param request Chứa version để kiểm tra Optimistic Locking.
     * @return DTO chứa thông tin phiếu xuất đã cập nhật trạng thái.
     */
    @PutMapping("/{id}/submit")
    public ResponseEntity<ExportReceiptResponse> submitForApproval(
            @PathVariable Long id,
            @Valid @RequestBody com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptSubmitRequest request) {
        ExportReceiptResponse response = exportReceiptService.submitForApproval(id, request);
        return ResponseEntity.ok(response);
    }

    // ponytail: Dùng thẳng Page<T> của Spring. Khai báo các tham số rỗng thay vì request param object.
    @GetMapping
    public org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listReceipts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String code,
            @org.springframework.data.web.PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return exportReceiptService.listReceipts(status, fromDate, toDate, warehouseId, code, pageable);
    }

    @GetMapping("/my")
    public org.springframework.data.domain.Page<com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptSummaryResponse> listMyReceipts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String code,
            @org.springframework.data.web.PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return exportReceiptService.listMyReceipts(status, fromDate, toDate, warehouseId, code, pageable);
    }

    // ponytail: Trả về DTO tái sử dụng ExportReceiptResponse, tránh viết DTO rườm rà.
    @GetMapping("/{id}/items")
    public java.util.List<ExportReceiptItemResponse> listItems(@PathVariable Long id) {
        return exportReceiptItemService.list(id);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ExportReceiptItemResponse> addItem(
            @PathVariable Long id, @Valid @RequestBody ExportReceiptItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exportReceiptItemService.add(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ExportReceiptItemResponse updateItem(
            @PathVariable Long id, @PathVariable Long itemId,
            @Valid @RequestBody ExportReceiptItemRequest request) {
        return exportReceiptItemService.update(id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        exportReceiptItemService.delete(id, itemId);
    }

    @GetMapping("/{id}/availability/{productId}")
    public StockAvailabilityResponse getAvailability(@PathVariable Long id, @PathVariable Long productId) {
        return exportReceiptItemService.availability(id, productId);
    }

    @GetMapping("/{id}/history")
    public java.util.List<ExportReceiptHistoryResponse> getHistory(@PathVariable Long id) {
        return exportReceiptService.getHistory(id);
    }

    /**
     * API Hủy Phiếu Xuất Nháp (T117)
     * Đánh dấu phiếu chuyển sang trạng thái DA_HUY (Soft Delete).
     *
     * @param id ID của phiếu xuất cần hủy.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{id}/draft")
    public ResponseEntity<Void> cancelDraft(@PathVariable Long id) {
        exportReceiptService.cancelDraft(id);
        return ResponseEntity.noContent().build();
    }
}
