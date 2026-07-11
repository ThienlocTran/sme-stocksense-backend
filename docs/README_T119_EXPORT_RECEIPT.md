# TÀI LIỆU API DANH SÁCH PHIẾU XUẤT KHO (T119)

## 1. API Danh Sách Phiếu
- **Endpoint:** `GET /api/v1/export-receipts` (Tất cả phiếu)
- **Endpoint:** `GET /api/v1/export-receipts/my` (Phiếu của tôi - dựa theo token)
- **Mục đích:** Liệt kê danh sách các phiếu xuất kho dưới dạng phân trang.

## 2. Logic Nghiệp Vụ
- **Ponytail applied:** API trả về trực tiếp `Page<T>` của Spring, không cần thiết phải bọc thêm các tầng DTO dư thừa. Nhận tham số qua `@RequestParam` đơn giản.
- **Filter:** Nhận query params để lọc động các trường tương ứng bằng `JpaSpecificationExecutor`.
- **Sắp xếp mặc định:** `createdAt` DESC. Client có thể tuỳ ý truyền tham số `&sort=totalAmount,asc` để đổi cách sắp xếp.

## 3. Cấu trúc Tham Số & Phản Hồi

### 📥 Parameters (Query)
- Phân trang: `page` (mặc định 0), `size` (mặc định 10).
- Lọc (Filters - Optional):
  - `status`: Lọc theo trạng thái phiếu (vd: `NHAP`, `CHO_DUYET_CAP_1`).
  - `warehouseId`: Lọc theo mã kho.
  - `code`: Lọc phiếu theo chuỗi mã phiếu (vd: `PXK-`).
  - `fromDate`, `toDate`: Lọc theo khoảng ngày (format `YYYY-MM-DD`).

### 📤 Phản Hồi Thành Công (Response)
- HTTP Status: `200 OK`
```json
{
  "content": [
    {
      "id": 10,
      "code": "PXK-20231024-ABC",
      "warehouseName": "Kho Tổng",
      "status": "NHAP",
      "totalAmount": 150000.00,
      "createdBy": "Nguyen Van A",
      "createdAt": "2023-10-24T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0
}
```
