package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptArrivalRequest;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
import com.smartflow.smestocksensebackend.dto.inbound.RejectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptHistoryResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;
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

    ImportReceiptPageResponse listReceipts(String status, Pageable pageable);

    /**
     * Lấy chi tiết thông tin phiếu nhập kho (T78).
     *
     * @param receiptId ID của phiếu nhập kho
     * @return Chi tiết phiếu nhập cùng danh sách sản phẩm bên trong
     */
    ImportReceiptDraftResponse getDetail(Long receiptId);

    ImportReceiptDraftResponse recordArrival(Long receiptId, ImportReceiptArrivalRequest request);

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

    DiscrepancyReportResponse approveDiscrepancyReport(Long receiptId, Long reportId);

    DiscrepancyReportResponse rejectDiscrepancyReport(Long receiptId, Long reportId, RejectImportReceiptRequest request);

    /**
     * Hoàn tất phiếu nhập kho (T104).
     * Bọc toàn bộ các khâu vào 1 giao dịch an toàn (ACID):
     * Lấy phiếu -> Update kiểm hàng (T100) -> Tăng tồn kho (T102) -> Ghi log (T103) -> Đổi status phiếu sang HOAN_THANH.
     *
     * @param receiptId ID của phiếu nhập kho cần hoàn tất
     * @param request Yêu cầu kiểm hàng chứa thông tin thực tế nhận được
     * @return Thông tin phiếu nhập kho sau khi hoàn tất
     */
    ImportReceiptDraftResponse completeImport(Long receiptId, InspectImportReceiptRequest request);

    /**
     * Lấy danh sách phiếu nhập đang chờ duyệt cho người quản lý (T91).
     * Chỉ MANAGER hoặc ADMIN được xem. Lọc theo các trạng thái chờ duyệt
     * (CHO_DUYET_CAP_1, CHO_DUYET_CAP_2). Nếu truyền {@code status} cụ thể thì chỉ lấy
     * đúng cấp chờ duyệt đó.
     *
     * @param status Trạng thái chờ duyệt cần lọc (tùy chọn, chỉ chấp nhận CHO_DUYET_CAP_1/CHO_DUYET_CAP_2)
     * @param pageable Thông tin phân trang
     * @return Danh sách phiếu nhập chờ duyệt phân trang
     */
    ImportReceiptPageResponse listPendingApproval(String status, Pageable pageable);

    /**
     * Lấy chi tiết phiếu nhập phục vụ duyệt/từ chối cho người quản lý (T92).
     * Khác với {@link #getDetail(Long)}, MANAGER/ADMIN được xem chi tiết phiếu của bất kỳ
     * nhân viên nào để kiểm tra trước khi duyệt hoặc từ chối.
     *
     * @param receiptId ID của phiếu nhập kho cần xem chi tiết
     * @return Chi tiết phiếu nhập cùng danh sách sản phẩm
     */
    ImportReceiptDraftResponse getApprovalDetail(Long receiptId);

    /**
     * Duyệt phiếu nhập kho theo cấp (T93).
     * <ul>
     *   <li>CHO_DUYET_CAP_1 → CHO_DUYET_CAP_2: ghi nhận người duyệt và ngày duyệt cấp 1.</li>
     *   <li>CHO_DUYET_CAP_2 → CHO_HANG_VE: ghi nhận người duyệt và ngày duyệt cấp 2.</li>
     * </ul>
     * Bước duyệt KHÔNG cộng tồn kho. Việc tăng tồn chỉ xảy ra khi hoàn tất phiếu (T104).
     *
     * @param receiptId ID của phiếu nhập kho cần duyệt
     * @return Phiếu nhập sau khi duyệt
     */
    ImportReceiptDraftResponse approve(Long receiptId);

    /**
     * Từ chối phiếu nhập kho đang chờ duyệt (T94).
     * Áp dụng cho phiếu ở trạng thái CHO_DUYET_CAP_1 hoặc CHO_DUYET_CAP_2.
     * Bắt buộc nhập lý do từ chối; phiếu chuyển sang trạng thái TU_CHOI.
     *
     * @param receiptId ID của phiếu nhập kho cần từ chối
     * @param request Lý do từ chối
     * @return Phiếu nhập sau khi bị từ chối
     */
    ImportReceiptDraftResponse reject(Long receiptId, RejectImportReceiptRequest request);

    /**
     * Lấy danh sách lịch sử duyệt của phiếu nhập (T93).
     * @param receiptId ID của phiếu nhập kho
     * @return Danh sách các bản ghi lịch sử duyệt
     */
    List<ImportReceiptHistoryResponse> getHistory(Long receiptId);
}
