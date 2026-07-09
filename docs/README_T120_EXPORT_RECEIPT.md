# TÀI LIỆU API CHI TIẾT PHIẾU XUẤT KHO (T120)

## 1. API Lấy Chi Tiết Phiếu
- **Endpoint:** `GET /api/v1/export-receipts/{id}`
- **Mục đích:** Lấy thông tin Master và danh sách sản phẩm chi tiết của một phiếu xuất.

## 2. Logic Nghiệp Vụ
- **Phân quyền (Authorization):** Chỉ **Người tạo phiếu** hoặc **Admin** mới được phép xem chi tiết. Nếu người khác truy cập, hệ thống trả về lỗi 403 Forbidden (`MissingRoleException`).
- **Edge Case - Đã Hủy:** Nếu phiếu ở trạng thái `HUY` (Soft Delete), hệ thống vẫn trả về bình thường (không văng 404), giúp FE hiển thị lịch sử phiếu bị hủy.
- **Ponytail applied:** API tái sử dụng toàn bộ luồng `ExportReceiptResponse` + `findById` có sẵn, hoàn toàn không đẻ thêm class DTO mới nào.

## 3. Cấu trúc Tham Số & Phản Hồi

### 📥 Parameters
- Path variable: `id` (Long) - ID của phiếu xuất.

### 📤 Phản Hồi Thành Công (Response)
- HTTP Status: `200 OK`
```json
{
  "id": 10,
  "code": "PXK-20231024-A1B2C3D4E5F6",
  "warehouseId": 1,
  "partnerId": 2,
  "status": "NHAP",
  "totalAmount": 150000.00,
  "note": "Xuất đi chi nhánh A",
  "createdAt": "2023-10-24T10:00:00",
  "details": [
    {
      "id": 100,
      "productId": 5,
      "productName": "Laptop Dell XPS 13",
      "quantity": 2,
      "unitPrice": 75000.00,
      "totalPrice": 150000.00,
      "note": "Giao nguyên seal"
    }
  ]
}
```

### ❌ Phản Hồi Lỗi (Errors)
- `404 Not Found`: Không tìm thấy phiếu xuất (ID không tồn tại).
- `403 Forbidden`: Người dùng không phải là Admin và cũng không phải là tác giả của phiếu này.
