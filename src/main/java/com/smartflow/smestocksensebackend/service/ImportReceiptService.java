package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
import org.springframework.data.domain.Pageable;

public interface ImportReceiptService {

    /**
     * Tạo nháp một phiếu nhập kho (T76).
     *
     * @param request Thông tin tạo phiếu nhập kho
     * @return Phiếu nhập kho đã tạo ở trạng thái DRAFT
     */
    ImportReceiptResponse createDraft(CreateImportReceiptRequest request);

    /**
     * Thêm sản phẩm vào phiếu nhập kho nháp (T77).
     *
     * @param receiptId ID của phiếu nhập kho nháp
     * @param request Thông tin sản phẩm cần thêm
     * @return Chi tiết sản phẩm vừa thêm kèm tổng giá trị phiếu nhập cập nhật
     */
    ImportReceiptItemResponse addItem(Long receiptId, AddImportReceiptItemRequest request);

    /**
     * Lưu tạm/cập nhật thông tin phiếu nhập kho nháp (T80).
     *
     * @param receiptId ID của phiếu nhập kho nháp
     * @param request Thông tin chi tiết cập nhật phiếu nhập
     * @return Thông tin toàn bộ phiếu nhập sau khi lưu tạm
     */
    ImportReceiptDraftResponse saveDraft(Long receiptId, SaveImportReceiptDraftRequest request);

    /**
     * Cập nhật thông tin phiếu nhập kho ở trạng thái DRAFT hoặc REJECTED (T81).
     *
     * @param receiptId ID của phiếu nhập kho
     * @param request Thông tin chi tiết cập nhật phiếu nhập
     * @return Thông tin toàn bộ phiếu nhập sau khi cập nhật
     */
    ImportReceiptDraftResponse updateEditable(Long receiptId, SaveImportReceiptDraftRequest request);

    /**
     * Hủy bỏ một phiếu nhập kho nháp (T82).
     *
     * @param receiptId ID của phiếu nhập kho
     * @return Phiếu nhập kho sau khi chuyển sang trạng thái CANCELLED
     */
    ImportReceiptDraftResponse cancelDraft(Long receiptId);

    /**
     * Gửi duyệt phiếu nhập kho nháp (T83).
     *
     * @param receiptId ID của phiếu nhập kho
     * @return Phiếu nhập kho sau khi chuyển sang trạng thái CHO_DUYET
     */
    ImportReceiptDraftResponse submitForApproval(Long receiptId);

    /**
     * Lấy danh sách phiếu nhập do nhân viên hiện tại tạo có phân trang và lọc theo trạng thái (T84).
     *
     * @param status Trạng thái phiếu nhập để lọc (tùy chọn)
     * @param pageable Thông tin phân trang
     * @return Danh sách phiếu nhập phân trang
     */
    ImportReceiptPageResponse listMyReceipts(String status, Pageable pageable);

    /**
     * Lấy chi tiết thông tin phiếu nhập kho (T78).
     *
     * @param receiptId ID của phiếu nhập kho
     * @return Chi tiết phiếu nhập cùng danh sách sản phẩm bên trong
     */
    ImportReceiptDraftResponse getDetail(Long receiptId);

    /**
     * Thực hiện kiểm hàng thực tế cho phiếu nhập kho (T100).
     * Ghi nhận số lượng thực đếm, tình trạng sản phẩm và đối chiếu khớp/lệch với chứng từ gốc.
     *
     * @param receiptId ID của phiếu nhập kho cần kiểm hàng
     * @param request Danh sách thông tin kiểm đếm thực nhận
     * @return Kết quả kiểm đếm được cập nhật vào phiếu nhập
     */
    ImportReceiptDraftResponse inspectReceipt(Long receiptId, InspectImportReceiptRequest request);

    /**
     * Lập biên bản chênh lệch cho phiếu nhập kho bị lệch số lượng sau kiểm hàng (T101).
     *
     * @param receiptId ID của phiếu nhập kho cần lập biên bản
     * @param request Lý do và hướng xử lý chênh lệch cho từng sản phẩm bị lệch
     * @return Biên bản chênh lệch đã lập thành công
     */
    DiscrepancyReportResponse createDiscrepancyReport(Long receiptId, CreateDiscrepancyReportRequest request);
}
