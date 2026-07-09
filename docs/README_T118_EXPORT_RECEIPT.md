# TÀI LIỆU TỔNG HỢP API GỬI DUYỆT PHIẾU XUẤT (T118)

## 1. API Gửi Phiếu Xuất Chờ Duyệt
- **Endpoint:** `PUT /api/v1/export-receipts/{id}/submit`
- **Mục đích:** Đẩy phiếu xuất kho từ trạng thái Nháp (Draft) sang luồng chờ duyệt cấp 1.

## 2. Logic Nghiệp Vụ (Business Rules)
- **Kiểm tra tồn kho nghiêm ngặt (Strict Inventory Check):** Tại chính thời điểm click "Gửi duyệt", hệ thống tự động quét lại toàn bộ sản phẩm trong phiếu. Nếu bất kỳ sản phẩm nào có số lượng yêu cầu vượt quá tồn kho thực tế, sẽ ném lỗi 400 Bad Request và yêu cầu người dùng phải chỉnh sửa lại Phiếu Nháp.
- **Chống ghi đè đồng thời (Optimistic Locking):** API yêu cầu truyền lên `version` của phiếu (được lấy lúc GET detail). Nếu `version` nhận được bị lệch so với dưới DB (nghĩa là có người khác đã sửa phiếu trước đó), API sẽ ném lỗi 409 Conflict.
- **Quyền hạn (Authorization):** Chỉ cho phép người đã tạo ra phiếu (hoặc Admin) được phép bấm Submit.
- **Vòng đời trạng thái (State Lifecycle):** Chỉ phiếu đang `NHAP` hoặc `TU_CHOI` mới được submit. Sau khi submit thành công, phiếu chuyển thành `CHO_DUYET_CAP_1`.

## 3. Cấu trúc Payload và Response

### 📦 Payload Yêu Cầu (Request)
```json
{
  "version": 1 // (Bắt buộc) Version của phiếu xuất hiện tại do FE đang giữ
}
```

### 📤 Phản Hồi Thành Công (Response)
- HTTP Status: `200 OK`
```json
{
  "id": 10,
  "code": "PXK-20231023-A1B2C3",
  "warehouseId": 1,
  "status": "CHO_DUYET_CAP_1",
  "totalAmount": 2250000.00,
  "details": [
    // ... danh sách sản phẩm ...
  ]
}
```

### 🔴 Phản Hồi Thất Bại Thường Gặp
- `400 Bad Request`: "Sản phẩm A chỉ còn 10 trong kho, không đủ xuất 15. Vui lòng sửa lại phiếu nháp."
- `409 Conflict`: "Phiếu xuất đã được cập nhật bởi người khác. Vui lòng tải lại trang." (Hoặc do trạng thái phiếu không hợp lệ).
- `403 Forbidden`: Người dùng không có quyền gửi duyệt phiếu này.
