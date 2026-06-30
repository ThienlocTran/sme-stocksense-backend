# Tổng Hợp Tính Năng API Ghi Nhận Hàng Về Thực Tế (Task T99)

Tài liệu này tổng hợp ngắn gọn các thông tin về chức năng và cấu trúc của API ghi nhận ngày hàng về thực tế cho phiếu nhập kho (Task T99).

Chi tiết giải thích từng dòng logic nghiệp vụ đã được viết trực tiếp dưới dạng chú thích (inline comments) trong từng file mã nguồn.

---

## 1. Các File Được Tạo / Sửa
- **Database Migration**:
  - `src/main/resources/db/migration/V12__add_actual_arrival_date.sql.sql`: Tạo cột `ngay_hang_ve` (timestamp) trong bảng `phieu_nhap_kho` để lưu ngày hàng về thực tế của phiếu nhập.
- **Thực thể (Entity)**:
  - `src/main/java/com/smartflow/smestocksensebackend/entity/ImportReceipt.java`: Thêm trường `actualArrivalDate` ánh xạ với cột `ngay_hang_ve` của cơ sở dữ liệu.
- **DTOs**:
  - `src/main/java/com/smartflow/smestocksensebackend/dto/inbound/ImportReceiptArrivalRequest.java`: DTO nhận yêu cầu cập nhật ngày hàng về thực tế với kiểm tra `@NotNull` cho `actualArrivalDate`.
  - `src/main/java/com/smartflow/smestocksensebackend/dto/inbound/ImportReceiptResponse.java`: Bổ sung trường `actualArrivalDate` trong Response.
  - `src/main/java/com/smartflow/smestocksensebackend/dto/inbound/ImportReceiptSummaryResponse.java`: Bổ sung trường `actualArrivalDate` trong Response danh sách/tóm tắt phiếu.
  - `src/main/java/com/smartflow/smestocksensebackend/dto/inbound/ImportReceiptDraftResponse.java`: Bổ sung trường `actualArrivalDate` trong chi tiết phản hồi và thêm các constructor overload để tránh làm ảnh hưởng hoặc gây lỗi biên dịch đối với các test cũ.
- **Nghiệp vụ (Service & Repository)**:
  - `src/main/java/com/smartflow/smestocksensebackend/service/ImportReceiptService.java`: Khai báo phương thức `recordArrival`.
  - `src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java`: Hiện thực logic nghiệp vụ cho `recordArrival` bao gồm:
    - Kiểm tra tài khoản nhân viên đang hoạt động.
    - Kiểm tra phân quyền: Chỉ cho phép role `ADMIN` hoặc `EMPLOYEE` (Nhân viên kho) thực hiện.
    - Truy vấn dữ liệu thực tế từ DB (không mock data).
    - **Logic kiểm tra trạng thái**: Chỉ cho phép cập nhật khi phiếu nhập đang ở trạng thái `CHO_HANG_VE`. Nếu khác trạng thái này sẽ trả về lỗi `409 Conflict`.
    - Cập nhật ngày hàng về thực tế (`ngay_hang_ve`) và chuyển trạng thái phiếu sang `CHO_KIEM_HANG`.
  - `src/main/java/com/smartflow/smestocksensebackend/repository/ImportReceiptDetailRepository.java`: Khai báo thêm `findByDocumentIdOrderByIdAsc(Long documentId)` để fix lỗi compile có sẵn của project.
- **Điều phối (Controller)**:
  - `src/main/java/com/smartflow/smestocksensebackend/controller/ImportReceiptController.java`: Cung cấp endpoint: `PUT /api/import-receipts/{receiptId}/arrival`.
- **Cấu hình Bảo mật (Security)**:
  - `src/main/java/com/smartflow/smestocksensebackend/config/SecurityConfig.java`: Phân quyền cho endpoint `PUT /api/import-receipts/*/arrival` cho phép role `ADMIN` và `EMPLOYEE` (Nhân viên kho) truy cập.
- **Kiểm thử (Unit Test)**:
  - `src/test/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImplTest.java`: Thêm 5 test cases bao quát tất cả các trường hợp thành công và thất bại cho nghiệp vụ ghi nhận hàng về.
  - `src/test/java/com/smartflow/smestocksensebackend/controller/ImportReceiptControllerTest.java`: Thêm 4 test cases cho endpoint kiểm thử xác thực và phân quyền của API.
  - `src/test/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptDetailServiceTest.java`: Sửa method helper `authenticateAs` bị lỗi compile sẵn.

---

## 2. API Endpoint Specification

### Ghi nhận hàng về thực tế
- **URL**: `PUT /api/import-receipts/{receiptId}/arrival`
- **Method**: `PUT`
- **Headers**:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- **Path Parameter**:
  - `receiptId` (Long): ID của phiếu nhập kho cần ghi nhận hàng về.
- **Request Body (JSON)**:
  ```json
  {
    "actualArrivalDate": "2026-06-22T10:00:00"
  }
  ```
- **Response (200 OK - JSON)**:
  Trả về chi tiết phiếu nhập kho đã cập nhật, ví dụ:
  ```json
  {
    "id": 123,
    "code": "PNK-20260618-ABC123DEF456",
    "warehouseId": 1,
    "warehouseName": "Kho trung tâm",
    "supplierId": 10,
    "supplierName": "Nhà cung cấp A",
    "createdById": 5,
    "createdByName": "Nguyễn Văn A",
    "submittedById": 5,
    "submittedByName": "Nguyễn Văn A",
    "submittedAt": "2026-06-18T16:00:00",
    "actualArrivalDate": "2026-06-22T10:00:00",
    "status": "CHO_KIEM_HANG",
    "totalAmount": 1250000.00,
    "note": "Phiếu nhập đã về kho",
    "details": [],
    "detailCount": 0,
    "updatedAt": "2026-06-22T10:45:00",
    "version": 2
  }
  ```

---

## 3. Các Phản Hồi Lỗi Đặc Thù
- **401 Unauthorized**: Khi không đính kèm JWT Token hoặc Token không hợp lệ.
- **403 Forbidden**: Khi người dùng không có vai trò `EMPLOYEE` (Nhân viên kho) hoặc `ADMIN`.
- **404 Not Found**: Khi không tìm thấy phiếu nhập kho với ID đã truyền (`Phiếu nhập không tồn tại.`).
- **409 Conflict**:
  - Khi phiếu nhập không ở trạng thái `CHO_HANG_VE` (`Chỉ được ghi nhận hàng về cho phiếu nhập ở trạng thái chờ hàng về (CHO_HANG_VE).`).
  - Khi có xung đột dữ liệu ghi đồng thời từ người dùng khác (`Phiếu nhập đã được cập nhật bởi một phiên làm việc khác.`).
- **400 Bad Request**: Khi request body trống hoặc thiếu trường `actualArrivalDate` (`Ngày hàng về thực tế không được để trống.`).
