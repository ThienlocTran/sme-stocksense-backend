package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptArrivalRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
import com.smartflow.smestocksensebackend.dto.inbound.RejectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptHistoryResponse;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import java.util.List;
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

    @GetMapping
    public ImportReceiptPageResponse listReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return importReceiptService.listReceipts(status, pageable);
    }

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

    /**
     * API danh sách phiếu nhập chờ duyệt cho quản lý (T91).
     * Chỉ MANAGER/ADMIN xem được. Lọc theo trạng thái chờ duyệt (CHO_DUYET_CAP_1/CHO_DUYET_CAP_2).
     *
     * @param page  Số trang (mặc định 0)
     * @param size  Kích thước trang (mặc định 10)
     * @param status Trạng thái chờ duyệt cần lọc (tùy chọn)
     * @return Danh sách phiếu nhập chờ duyệt phân trang
     */
    @GetMapping("/pending-approval")
    public ImportReceiptPageResponse listPendingApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("submittedAt"), Sort.Order.asc("id"))
        );
        return importReceiptService.listPendingApproval(status, pageable);
    }

    /**
     * API xem chi tiết phiếu nhập chờ duyệt cho quản lý (T92).
     * Cho phép MANAGER/ADMIN xem chi tiết phiếu của bất kỳ nhân viên nào trước khi duyệt/từ chối.
     *
     * @param receiptId ID của phiếu nhập kho
     * @return Chi tiết phiếu nhập cùng danh sách sản phẩm
     */
    @GetMapping("/{receiptId}/approval-detail")
    public ImportReceiptDraftResponse getApprovalDetail(@PathVariable Long receiptId) {
        return importReceiptService.getApprovalDetail(receiptId);
    }

    /**
     * API duyệt phiếu nhập kho theo cấp (T93).
     * Cấp 1: CHO_DUYET_CAP_1 → CHO_DUYET_CAP_2. Cấp 2: CHO_DUYET_CAP_2 → CHO_HANG_VE.
     * Không cộng tồn kho ở bước duyệt.
     *
     * @param receiptId ID của phiếu nhập kho cần duyệt
     * @return Phiếu nhập sau khi duyệt
     */
    @PutMapping("/{receiptId}/approve")
    public ImportReceiptDraftResponse approve(@PathVariable Long receiptId) {
        return importReceiptService.approve(receiptId);
    }

    /**
     * API từ chối phiếu nhập kho đang chờ duyệt (T94).
     * Bắt buộc nhập lý do từ chối; phiếu chuyển sang trạng thái TU_CHOI.
     *
     * @param receiptId ID của phiếu nhập kho cần từ chối
     * @param request Lý do từ chối
     * @return Phiếu nhập sau khi bị từ chối
     */
    @PutMapping("/{receiptId}/reject")
    public ImportReceiptDraftResponse reject(
            @PathVariable Long receiptId,
            @Valid @RequestBody RejectImportReceiptRequest request
    ) {
        return importReceiptService.reject(receiptId, request);
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
     * API ghi nhận hàng về thực tế cho phiếu nhập kho.
     * Chỉ phiếu đang ở trạng thái CHO_HANG_VE mới được ghi nhận; sau khi ghi nhận
     * phiếu chuyển sang CHO_KIEM_HANG.
     *
     * @param receiptId ID của phiếu nhập kho
     * @param request DTO chứa ngày hàng về thực tế
     * @return Thông tin chi tiết phiếu sau khi cập nhật
     */
    @PutMapping("/{receiptId}/arrival")
    public ImportReceiptDraftResponse recordArrival(
            @PathVariable Long receiptId,
            @Valid @RequestBody ImportReceiptArrivalRequest request
    ) {
        return importReceiptService.recordArrival(receiptId, request);
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

    @PostMapping("/{receiptId}/discrepancy-reports/{reportId}/approve")
    public DiscrepancyReportResponse approveDiscrepancyReport(
            @PathVariable Long receiptId,
            @PathVariable Long reportId
    ) {
        return importReceiptService.approveDiscrepancyReport(receiptId, reportId);
    }

    @PostMapping("/{receiptId}/discrepancy-reports/{reportId}/reject")
    public DiscrepancyReportResponse rejectDiscrepancyReport(
            @PathVariable Long receiptId,
            @PathVariable Long reportId,
            @Valid @RequestBody RejectImportReceiptRequest request
    ) {
        return importReceiptService.rejectDiscrepancyReport(receiptId, reportId, request);
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

    @GetMapping("/{receiptId}/history")
    public List<ImportReceiptHistoryResponse> getHistory(@PathVariable Long receiptId) {
        return importReceiptService.getHistory(receiptId);
    }
}
