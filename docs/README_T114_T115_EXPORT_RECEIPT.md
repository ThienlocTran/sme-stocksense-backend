# TÀI LIỆU TỔNG HỢP API PHIẾU XUẤT KHO (T114, T115, T116)

## 1. API Tạo Phiếu Nháp (T114)
- **Endpoint:** `POST /api/v1/export-receipts/draft`
- **Mục đích:** Khởi tạo một Phiếu Xuất Nháp mới (trạng thái `NHAP`).
- **Luồng xử lý (Logic):**
  - Sinh mã tự động `PXK-YYYYMMDD-XXXX`.
  - Validate nghiêm ngặt `so_luong` xuất không được lớn hơn `ton_hien_tai` trong kho.
  - Chặn nếu mảng chi tiết chứa 2 dòng cùng 1 `productId`.

## 2. API Cập nhật/Lưu Nháp Phiếu Xuất (T115, T116)
- **Endpoint:** `PUT /api/v1/export-receipts/{id}/draft`
- **Mục đích:** Cập nhật lại toàn bộ thông tin phiếu xuất và danh sách sản phẩm.
- **Luồng xử lý (Logic):**
  - **Quyền:** Chỉ người tạo ra phiếu mới được phép sửa.
  - **Trạng thái (State):** Chỉ được sửa khi phiếu đang ở trạng thái `NHAP` (Draft) hoặc `TU_CHOI` (Bị từ chối). Cấm sửa khi đang chờ duyệt.
  - **Replace All:** Xóa toàn bộ danh sách chi tiết cũ trong DB và insert danh sách mới từ Request.
  - **Validate Tồn Kho:** Vẫn phải kiểm tra tồn kho tại thời điểm lưu nháp xem có đủ hàng không.

### 📦 Payload Dùng Chung (Cho cả POST và PUT)
```json
{
  "warehouseId": 1, 
  "partnerId": 2, // (Tùy chọn) 
  "note": "Xuất hàng gấp", 
  "details": [
    {
      "productId": 101,
      "quantity": 15,
      "unitPrice": 150000,
      "note": "Hàng lấy lô mới" 
    }
  ]
}
```

### 📤 Phản Hồi Thành Công (Response)
```json
{
  "id": 10,
  "code": "PXK-20231023-A1B2C3",
  "warehouseId": 1,
  "partnerId": 2,
  "status": "NHAP",
  "totalAmount": 2250000.00,
  "details": [ ...danh sách sản phẩm cập nhật... ]
}
```
