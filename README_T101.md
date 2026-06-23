# T101: TỔNG HỢP TÍNH NĂNG LẬP BIÊN BẢN CHÊNH LỆCH NHẬP KHO

Tài liệu này tổng hợp toàn bộ các thiết kế cấu trúc database, quy tắc nghiệp vụ, cấu trúc mã nguồn và hướng dẫn kiểm thử cho API lập biên bản chênh lệch kiểm đếm khi nhập kho.

---

## 1. Giới thiệu chung (Overview)
Tính năng lập biên bản chênh lệch cho phép ghi nhận chênh lệch số lượng thực tế kiểm đếm so với số lượng trên chứng từ gốc của phiếu nhập kho (`Import Receipt`). Biên bản này là cơ sở pháp lý để đối soát làm việc với nhà cung cấp hoặc xử lý hao hụt nội bộ.

## 2. Luồng nghiệp vụ liên quan (Flow)
1. **T100 (Kiểm hàng)**: Khi kiểm đếm hàng thực tế, nếu số lượng kiểm đếm thực tế không khớp với số lượng trên chứng từ, các dòng chi tiết của phiếu nhập sẽ được đánh dấu trạng thái dòng (`row_status`) là `CHENH_LECH`. Phiếu chuyển sang trạng thái `CHO_KIEM_HANG`.
2. **T101 (Tạo biên bản chênh lệch)**:
   - Nhân viên hoặc Quản trị viên gọi API lập biên bản chênh lệch cho phiếu nhập đang ở trạng thái `CHO_KIEM_HANG`.
   - Hệ thống tự động lọc ra các chi tiết dòng có trạng thái `CHENH_LECH`.
   - Người gọi cung cấp ghi chú chung (`note`), lý do chênh lệch (`reason`), và hướng xử lý đề xuất (`action`) cho mỗi sản phẩm bị lệch.
   - Hệ thống tự tạo mã biên bản theo định dạng: `BBCL-[Mã Phiếu Nhập]`.
   - Sau khi lưu thành công, biên bản chênh lệch được lưu trữ và liên kết khóa ngoại với phiếu nhập kho.

---

## 3. Cấu trúc Cơ sở dữ liệu (Database Schema)
Các bảng được tạo mới qua Flyway migration [V11__create_discrepancy_report_tables.sql](src/main/resources/db/migration/V11__create_discrepancy_report_tables.sql):

### A. Bảng `bien_ban_chenh_lech`
Lưu trữ thông tin đầu phiếu của biên bản chênh lệch (quan hệ `1-1` với phiếu nhập).
- `id` (BIGINT, PK): Khóa chính tự tăng.
- `phieu_nhap_id` (BIGINT, Unique, FK): Liên kết với bảng `phieu_nhap_kho`.
- `ma_bien_ban` (VARCHAR(50), Unique): Mã biên bản định dạng `BBCL-[Mã Phiếu Nhập]`.
- `ngay_lap` (TIMESTAMP): Ngày lập biên bản.
- `nguoi_lap_id` (BIGINT, FK): Tham chiếu đến nhân viên lập phiếu (`nhan_vien`).
- `ghi_chu` (VARCHAR(255)): Ghi chú chung của biên bản.
- `version` (BIGINT): Sử dụng cho khóa lạc quan (Optimistic Locking).

### B. Bảng `chi_tiet_bien_ban_chenh_lech`
Lưu trữ chi tiết các sản phẩm bị chênh lệch số lượng.
- `id` (BIGINT, PK): Khóa chính tự tăng.
- `bien_ban_id` (BIGINT, FK): Tham chiếu bảng `bien_ban_chenh_lech`.
- `san_pham_id` (BIGINT, FK): Tham chiếu bảng `san_pham`.
- `so_luong_chung_tu` (INT): Số lượng theo chứng từ gốc.
- `so_luong_thuc_te` (INT): Số lượng thực tế nhận được từ bước kiểm đếm.
- `so_luong_lech` (INT): Số lượng chênh lệch (Thực tế - Chứng từ).
- `ly_do` (VARCHAR(255)): Lý do chênh lệch của dòng sản phẩm này.
- `huong_xu_ly` (VARCHAR(255)): Hướng xử lý đề xuất cho dòng sản phẩm này.

---

## 4. API Endpoints
**Lập biên bản chênh lệch:**
- **Method / Path**: `POST /api/import-receipts/{receiptId}/discrepancy-report`
- **Quyền truy cập (Security)**: Chỉ cho phép tài khoản có vai trò `ADMIN` hoặc `EMPLOYEE`. Chặn các vai trò khác như `MANAGER` (HTTP 403 Forbidden).
- **Request Body (JSON)**:
  ```json
  {
    "note": "Biên bản ghi nhận thiếu hàng cà phê",
    "items": [
      {
        "productId": 20,
        "reason": "Nhà cung cấp giao thiếu hàng",
        "action": "Yêu cầu giao bù trong 3 ngày"
      }
    ]
  }
  ```
- **Response Body (201 Created)**:
  Trả về thông tin biên bản chênh lệch cùng danh sách chi tiết các sản phẩm chênh lệch được lập.

  ```json
  {
    "id": 1,
    "code": "BBCL-PNK-20240001",
    "note": "Biên bản ghi nhận thiếu hàng cà phê",
    "createdById": 1,
    "createdByName": "Nguyễn Văn A",
    "createdAt": "2024-06-22T10:30:00",
    "details": [
      {
        "productId": 20,
        "discrepancyQuantity": -2,
        "reason": "Nhà cung cấp giao thiếu hàng",
        "action": "Yêu cầu giao bù trong 3 ngày"
      }
    ]
  }
  ```

---

## 5. Quy tắc nghiệp vụ & Kiểm tra (Business Validation Rules)
1. **Trạng thái phiếu**: Phiếu nhập kho liên kết bắt buộc phải ở trạng thái `CHO_KIEM_HANG`.
2. **Kiểm tra chênh lệch thực tế**: Phiếu nhập phải chứa ít nhất một sản phẩm có trạng thái kiểm hàng là `CHENH_LECH`. Nếu tất cả đều khớp mà gọi API này sẽ báo lỗi `400 Bad Request`.
3. **Giới hạn số lượng biên bản**: Mỗi phiếu nhập chỉ được phép lập tối đa một biên bản chênh lệch. Nếu đã lập trước đó, hệ thống báo lỗi `409 Conflict`.
4. **Ràng buộc độ dài ký tự**:
   - `note` của biên bản không vượt quá 255 ký tự.
   - `reason` và `action` của từng dòng sản phẩm chênh lệch không vượt quá 255 ký tự.
   - Danh sách sản phẩm chênh lệch (`items`) không được phép để trống.

---

## 6. Cấu trúc các file mã nguồn triển khai (Code Structure)
- **Entities**:
  - [DiscrepancyReport.java](src/main/java/com/smartflow/smestocksensebackend/entity/DiscrepancyReport.java)
  - [DiscrepancyReportDetail.java](src/main/java/com/smartflow/smestocksensebackend/entity/DiscrepancyReportDetail.java)
- **Repositories**:
  - [DiscrepancyReportRepository.java](src/main/java/com/smartflow/smestocksensebackend/repository/DiscrepancyReportRepository.java)
  - [DiscrepancyReportDetailRepository.java](src/main/java/com/smartflow/smestocksensebackend/repository/DiscrepancyReportDetailRepository.java)
- **DTOs (Lombok Classes)**:
  - [CreateDiscrepancyReportRequest.java](src/main/java/com/smartflow/smestocksensebackend/dto/inbound/CreateDiscrepancyReportRequest.java)
  - [CreateDiscrepancyReportItemRequest.java](src/main/java/com/smartflow/smestocksensebackend/dto/inbound/CreateDiscrepancyReportItemRequest.java)
  - [DiscrepancyReportResponse.java](src/main/java/com/smartflow/smestocksensebackend/dto/inbound/DiscrepancyReportResponse.java)
  - [DiscrepancyReportDetailResponse.java](src/main/java/com/smartflow/smestocksensebackend/dto/inbound/DiscrepancyReportDetailResponse.java)
- **Controller & Service layer**:
  - [ImportReceiptController.java](src/main/java/com/smartflow/smestocksensebackend/controller/ImportReceiptController.java) (Khai báo endpoint `/api/import-receipts/{receiptId}/discrepancy-report`)
  - [ImportReceiptService.java](src/main/java/com/smartflow/smestocksensebackend/service/ImportReceiptService.java) (Khai báo interface nghiệp vụ)
  - [ImportReceiptServiceImpl.java](src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java) (Triển khai logic nghiệp vụ tại method `createDiscrepancyReport`)
- **Unit Tests**:
  - [ImportReceiptDiscrepancyReportControllerTest.java](src/test/java/com/smartflow/smestocksensebackend/controller/ImportReceiptDiscrepancyReportControllerTest.java) (Test endpoint và bảo mật)
  - [ImportReceiptDetailServiceTest.java](src/test/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptDetailServiceTest.java) (Test nghiệp vụ, tính toán số lượng lệch và các logic ràng buộc)

---

## 7. Hướng dẫn chạy kiểm thử (Testing)
Để chạy tự động kiểm thử toàn bộ dự án và xác nhận chất lượng mã nguồn:
```bash
.\mvnw.cmd test
```
